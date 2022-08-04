package Sinus46.AI;

import Sinus46.Schnapsen.Card;
import Sinus46.Schnapsen.Trick;

import java.util.List;
import java.util.Random;

public class SimpleBot {
    public static void play(Trick trick) {
        List<Card> filtered = null;
        Trick.Hand hand = trick.getHand(trick.player());
        if (trick.isLeading()) {
            if (hand.canExchange()) {
                trick.exchange();
                return;
            }
            filtered = hand.content().stream()
                    .filter(hand::isPaired).toList();
        }
        if (filtered == null || filtered.size() == 0){
            filtered = hand.playableCards().stream().filter(card -> !hand.isPaired(card)).toList();
        }
        if (filtered.size() == 0) filtered = hand.playableCards();
        try {
            trick.play(filtered.get(new Random().nextInt(filtered.size())));
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
