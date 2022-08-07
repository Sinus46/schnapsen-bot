package Sinus46;

import Sinus46.AI.SimpleBot;
import Sinus46.AI.Tree;
import Sinus46.Schnapsen.Trick;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;

public class NeuralMain {

    public static void main(String[] args) {
        int netScore = 0;
        for (int i = 0; i < 1000; i++) {
            Trick trick = new Trick();
            while (trick.ergebnis() == 0) {
                if (trick.player() == 0) {
                    Integer best = Tree.normalAI(trick, new int[]{7, 7}, 1e+5).entrySet().stream()
                            .max(Comparator.comparingInt(Map.Entry::getValue)).get().getKey();
                    switch (best) {
                        case -2 -> trick.talonsperre();
                        case -1 -> trick.exchange();
                        default -> trick.play(best);
                    }
                } else {
                    Integer best = Tree.godlyAI(trick, new int[]{7, 7}, 1e+5).entrySet().stream()
                            .max(Comparator.comparingInt(Map.Entry::getValue)).get().getKey();
                    switch (best) {
                        case -2 -> trick.talonsperre();
                        case -1 -> trick.exchange();
                        default -> trick.play(best);
                    }
                }
            }
            netScore += (trick.player() * 2 - 1) * trick.ergebnis();
        }
        System.out.println(netScore);
    }
}
