package Sinus46.App;

import Sinus46.AI.Tree;
import Sinus46.Schnapsen.Card;
import Sinus46.Schnapsen.Trick;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;

public class Game extends JFrame implements ActionListener {
    private Trick trick;
    private int vorhand = 0;
    private final ArrayList<int[]> history = new ArrayList<>();
    private Card ansage = null;
    private int ansagePlayer = 0;
    private final String aiType;

    private final String SPERRE = "sperre";
    private final String TAUSCH = "tausch";

    public Game(String aiType) {
        super("Schnapsenpartie");
        this.aiType = aiType;
        setSize(500, 500);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
        trick = new Trick(false);
        play();
    }

    public void play() {
        update(trick.player() == vorhand);
        if (Arrays.stream(scores()).anyMatch((s -> s <= 0))) {
            return;
        }
        if (trick.ergebnis() != 0) {
            int[] result = new int[]{0, 0};
            result[(trick.player() + vorhand) % 2] = trick.ergebnis();
            history.add(result);
            vorhand = (vorhand + 1) % 2;
            trick = new Trick(false);
            ansage = null;
            play();
            return;
        }
        new Thread(() -> {
            if (trick.player() == 1 - vorhand) {
                int best;
                switch (aiType) {
                    case App.NORMAL_AI -> best = Tree.normalAI(trick, scores(), 3e+9).entrySet().stream()
                            .max(Comparator.comparingInt(Map.Entry::getValue)).get().getKey();
                    case App.UNFAIR_AI -> best = Tree.unfairAI(trick, scores(), 3e+9).entrySet().stream()
                            .max(Comparator.comparingInt(Map.Entry::getValue)).get().getKey();
                    case App.GODLY_AI -> best = Tree.godlyAI(trick, scores(), 3e+9).entrySet().stream()
                            .max(Comparator.comparingInt(Map.Entry::getValue)).get().getKey();
                    default -> throw new RuntimeException();
                }
                if (best >= 0) {
                    Card bestCard = trick.getHand(trick.player()).content().get(best);
                    if (trick.isLeading()) {
                        ansage = trick.getHand(trick.player()).pairOf(bestCard);
                        if (ansage != null) {
                            ansagePlayer = 1;
                        }
                    }
                }
                switch (best) {
                    case -2 -> trick.talonsperre();
                    case -1 -> trick.exchange();
                    default -> trick.play(best);
                }
                play();
            }}).start();
    }

    private void update(boolean enabled) {
        try {
            JPanel main = new JPanel(new BorderLayout());

            Box hand = new Box(BoxLayout.X_AXIS);
            for (Card card : trick.getHand(vorhand).content()) {
                hand.add(createButton(card, enabled));
                hand.add(Box.createHorizontalStrut(5));
            }
            JPanel bottom = new JPanel();
            bottom.add(hand);

            Box options = new Box(BoxLayout.Y_AXIS);
            if (trick.getHand(vorhand).canExchange()) {
                JButton btn = new JButton("Trumpfunter austauschen");
                btn.setActionCommand(TAUSCH);
                btn.addActionListener(this);
                btn.setEnabled(enabled);
                btn.setSize(new Dimension(80, 25));
                options.add(btn);
                options.add(Box.createVerticalStrut(7));
            }
            if (!trick.talonGeschlossen()) {
                JButton btn = new JButton("Talon zusperren");
                btn.setActionCommand(SPERRE);
                btn.addActionListener(this);
                btn.setEnabled(enabled);
                btn.setSize(new Dimension(80, 25));
                options.add(btn);
            }
            bottom.add(options);

            Box infoPanel = new Box(BoxLayout.Y_AXIS);
            JPanel trumpPanel = new JPanel();
            trumpPanel.add(new JLabel("Trumpf: "));
            if (trick.talonSize() == 0) {
                trumpPanel.add(new JLabel(Card.parseSuit(trick.getTrump().suit(), false)));
            } else {
                Icon trump = new ImageIcon(fetchCard(trick.getTrump().toString()));
                trumpPanel.add(new JLabel(trump));
            }
            infoPanel.add(trumpPanel);
            switch (trick.talonSize()) {
                case -1 -> infoPanel.add(new JLabel("Talon: GESPERRT"));
                case 0 -> infoPanel.add(new JLabel("Talon: Aus"));
                default -> infoPanel.add(new JLabel("Talon: " + trick.talonSize() + " ??brig"));
            }

            JPanel top = new JPanel();
            JLabel aiLabel = new JLabel(aiType);
            top.add(aiLabel);

            JPanel center = new JPanel(new BorderLayout());
            JPanel trickDisplay = new JPanel();
            String[] cards = trick.toString().split("(?<=[????????????-])");
            for (String card: cards) {
                trickDisplay.add(card.equals("-") ? new JLabel() : new JLabel(new ImageIcon(fetchCard(card))));
            }
            center.add(trickDisplay, BorderLayout.CENTER);

            if (ansage != null) {
                JPanel ansagePanel = new JPanel();
                ansagePanel.add(new JLabel(new ImageIcon(fetchCard(ansage.toString()))));
                center.add(ansagePanel, ansagePlayer == 1 ? BorderLayout.NORTH : BorderLayout.SOUTH);
            }

            TableModel model = new AbstractTableModel() {
                @Override
                public int getRowCount() {
                    return history.size() + 1;
                }

                @Override
                public int getColumnCount() {
                    return 2;
                }

                @Override
                public String getColumnName(int column) {
                    if (column == 0) {
                        return "Du";
                    }
                    if (column == 1) {
                        return aiType + " AI";
                    }
                    throw new RuntimeException();
                }

                @Override
                public Object getValueAt(int rowIndex, int columnIndex) {
                    if (rowIndex == history.size()) {
                        return scores()[columnIndex];
                    }
                    return history.get(rowIndex)[columnIndex];
                }

                @Override
                public boolean isCellEditable(int rowIndex, int columnIndex) {
                    return false;
                }
            };
            Box scoreBoard = new Box(BoxLayout.Y_AXIS);
            scoreBoard.setPreferredSize(new Dimension(100, Integer.MAX_VALUE));
            JTable table = new JTable(model);
            table.setFocusable(false);
            table.setRowSelectionAllowed(false);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
            table.getTableHeader().setResizingAllowed(false);
            table.getTableHeader().setReorderingAllowed(false);
            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setPreferredSize(new Dimension(100, 100));
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scoreBoard.add(scrollPane);
            scoreBoard.add(new JLabel("" + trick.scores()[vorhand]));

            main.add(center, BorderLayout.CENTER);
            main.add(bottom, BorderLayout.SOUTH);
            main.add(infoPanel, BorderLayout.EAST);
            main.add(top, BorderLayout.NORTH);
            main.add(scoreBoard, BorderLayout.WEST);
            setContentPane(main);
            validate();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private BufferedImage fetchCard(String card) throws IOException {
        return ImageIO.read(Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("cards/" + card + ".png"), "Card " + card + " does not exist"));
    }

    private JButton createButton(Card card, boolean enabled) throws IOException {
        Icon icon = new ImageIcon(fetchCard(card.toString()));
        JButton btn = new JButton();
        btn.setActionCommand(card.toString());
        btn.addActionListener(this);
        btn.setEnabled(enabled && trick.getHand(vorhand).isPlayable(card));
        btn.setIcon(icon);
        btn.setMargin(new Insets(0, 0, 0, 0));
        return btn;
    }


    @Override
    public void actionPerformed(ActionEvent e) {
        switch (e.getActionCommand()) {
            case SPERRE -> trick.talonsperre();
            case TAUSCH -> trick.exchange();
            default -> {
                trick.play(Card.evaluate(e.getActionCommand()));
                ansage = trick.getHand(vorhand).pairOf(Card.evaluate(e.getActionCommand()));
                if (ansage != null) {
                    ansagePlayer = 0;
                }
            }
        }
        play();
    }

    private int[] scores() {
        return new int[]{7 - history.stream().mapToInt((a -> a[0])).sum(), 7 - history.stream().mapToInt((a -> a[1])).sum()};
    }
}
