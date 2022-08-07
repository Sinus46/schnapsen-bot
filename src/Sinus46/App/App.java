package Sinus46.App;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

public class App extends JFrame implements ActionListener {
    public static final String NORMAL_AI = "Normal";
    public static final String UNFAIR_AI = "Unfair";
    public static final String GODLY_AI = "Godly";
    private static App instance = null;

    public static void init(){
        if (instance == null) {
            instance = new App();
        }
    }

    private App() {
        super("Schnapsen");
        setSize(500, 500);
        setResizable(false);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
        setContentPane(mainMenu());
        validate();
    }

    private Container mainMenu() {
        JPanel pane = new JPanel();

        JButton normal = new JButton(NORMAL_AI);
        normal.setMaximumSize(new Dimension(100, 30));
        JButton hard = new JButton(UNFAIR_AI);
        hard.setMaximumSize(new Dimension(100, 30));
        JButton godly = new JButton(GODLY_AI);
        godly.setMaximumSize(new Dimension(100, 30));
        normal.addActionListener(this);
        hard.addActionListener(this);
        godly.addActionListener(this);

        Box box = new Box(BoxLayout.Y_AXIS);
        box.add(normal);
        box.add(Box.createVerticalStrut(5));
        box.add(hard);
        box.add(Box.createVerticalStrut(5));
        box.add(godly);

        pane.add(box);
        SpringLayout manager = new SpringLayout();
        manager.putConstraint(SpringLayout.VERTICAL_CENTER, box, 0, SpringLayout.VERTICAL_CENTER, pane);
        manager.putConstraint(SpringLayout.HORIZONTAL_CENTER, box, 0, SpringLayout.HORIZONTAL_CENTER, pane);
        pane.setLayout(manager);

        return pane;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        setVisible(false);
        new Game(e.getActionCommand());
    }
}
