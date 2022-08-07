package Sinus46.App;

import Sinus46.AI.Tree;
import Sinus46.Schnapsen.Card;
import Sinus46.Schnapsen.Trick;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

public class Game extends JFrame implements ActionListener {
    private Trick trick;
    private int vorhand = 0;
    private final int[] scores = {7, 7};
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
        update(trick.player() == 0);
        if (trick.ergebnis() != 0) {
            scores[(trick.player() + vorhand) % 2] -= trick.ergebnis();
            vorhand = (vorhand + 1) % 2;
            trick = new Trick(false);
            play();
            return;
        }
        new Thread(() -> {
            if (trick.player() == 1) {
                Integer best = Tree.simulate(trick, scores, 3e+9).entrySet().stream()
                        .max(Comparator.comparingInt(Map.Entry::getValue)).get().getKey();
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
            for (Card card : trick.getHand(0).content()) {
                hand.add(createButton(card, enabled));
                hand.add(Box.createHorizontalStrut(5));
            }
            JPanel bottom = new JPanel();
            bottom.add(hand);

            Box options = new Box(BoxLayout.Y_AXIS);
            if (trick.getHand(0).canExchange()) {
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

            JPanel trumpPanel = new JPanel();
            Icon trump = new ImageIcon(fetchCard(trick.getTrump().toString()));
            trumpPanel.add(new JLabel("Trumpf: "));
            trumpPanel.add(new JLabel(trump));

            JPanel top = new JPanel();
            JLabel aiLabel = new JLabel(aiType);
            top.add(aiLabel);

            JPanel center = new JPanel(new BorderLayout());
            JPanel trickDisplay = new JPanel();
            String[] cards = trick.toString().split("(?<=[♥♠♣♦-])");
            for (String card: cards) {
                trickDisplay.add(card.equals("-") ? new JLabel() : new JLabel(new ImageIcon(fetchCard(card))));
            }
            center.add(trickDisplay, BorderLayout.CENTER);

            if (ansage != null) {
                JPanel ansagePanel = new JPanel();
                ansagePanel.add(new JLabel(new ImageIcon(fetchCard(ansage.toString()))));
                center.add(ansagePanel, ansagePlayer == 1 ? BorderLayout.NORTH : BorderLayout.SOUTH);
            }

            JPanel pointPanel = new JPanel();
            pointPanel.add(new JLabel("Deine Punktenanzahl: " + trick.scores()[0]));

            main.add(center, BorderLayout.CENTER);
            main.add(bottom, BorderLayout.SOUTH);
            main.add(trumpPanel, BorderLayout.EAST);
            main.add(top, BorderLayout.NORTH);
            main.add(pointPanel, BorderLayout.WEST);
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
        btn.setEnabled(enabled);
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
                ansage = trick.getHand(trick.player()).pairOf(Card.evaluate(e.getActionCommand()));
                if (ansage != null) {
                    ansagePlayer = 0;
                }
            }
        }
        play();
    }


}
