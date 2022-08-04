package Sinus46.AI;

import Sinus46.Schnapsen.Card;
import Sinus46.Schnapsen.Trick;


import java.util.HashMap;
import java.util.Random;

public class Tree {


    public static HashMap<Integer, Integer> simulate(Trick trick, int[] scores, double thinkingTime){
        long time = System.nanoTime();
        HashMap<Integer, Integer> results = new HashMap<>();
        for (int i = -2; i < 5; i++) {
            results.put(i, 0);
        }
        for (int i = 0; i < 100; i++){
            int best = -3;
            long t = System.nanoTime();
            for (int j = 0; System.nanoTime() - t < thinkingTime/100; j++) {
                best = bestMove(trick.randomizedCopy(trick.player()), j, scores);
            }
            results.put(best, results.get(best) == null ? 1: results.get(best) + 1);
        }
//        System.out.println("Time required: " + (System.nanoTime() - time)/1000000000);
//        System.out.println("Results: " + results);
//        System.out.println("Cards: " + trick.getHand(trick.player()));
        return results;
    }

    private static int bestMove(Trick trick, int maxDepth, int[] scores){
        int best = -20;
        int bestEval = Integer.MIN_VALUE;
        int aiPos = trick.player();
        for (Card card:trick.getHand(aiPos).playableCards()) {
            Trick copy = trick.copy();
            copy.play(card);
            int eval = miniMax(copy, aiPos, -20, 20, 0, maxDepth);
            if (eval > 0){
                eval = Math.min(eval, scores[1] * 100);
            } else {
                eval = Math.max(eval, scores[0] * -100);
            }
            if (eval > bestEval || (eval == bestEval && new Random().nextInt(100) > 72)){
                best = trick.getHand(aiPos).content().indexOf(card);
                bestEval = eval;
            }
        }
        if (trick.getHand(aiPos).canExchange()){
            Trick copy = trick.copy();
            copy.exchange();
            int eval = miniMax(copy, aiPos, -20, 20, 0, maxDepth);
            if (eval > bestEval){
                best = -1;
                bestEval = eval;
            }
        }
        if (!trick.talonGeschlossen() && trick.isLeading()){
            Trick copy = trick.copy();
            copy.talonsperre();
            int eval = miniMax(copy, aiPos, -20, 20, 0, maxDepth);
            if (eval > bestEval){
                best = -2;
            }
        }
        return best;
    }

    private static int miniMax(Trick trick, int aiPos, int alpha, int beta, int depth, int maxDepth){
        if (trick.ergebnis() > 0){
            return trick.ergebnis() * (trick.player() == aiPos ? 100 : -100);
        }
        if (depth == maxDepth){
            while (trick.ergebnis() == 0) {
                SimpleBot.play(trick);
            }
            return trick.ergebnis() * (trick.player() == aiPos ? 100 : -100);
        }
        int bestEval;
        if (trick.player() == aiPos) bestEval = -20;
        else bestEval = 20;
        for (Card card:trick.getHand(trick.player()).playableCards()) {
            Trick copy = trick.copy();
            copy.play(card);
            int eval = miniMax(copy, aiPos, alpha, beta, depth + 1, maxDepth);
            if (trick.player() == aiPos){
                bestEval = Math.max(eval, bestEval);
                alpha = Math.max(bestEval, alpha);
            }else {
                bestEval = Math.min(eval, bestEval);
                beta = Math.min(bestEval, beta);
            }
            if (beta <= alpha) return bestEval;
        }
        if (trick.getHand(trick.player()).canExchange()){
            Trick copy = trick.copy();
            copy.exchange();
            int eval = miniMax(copy, aiPos, alpha, beta, depth + 1, maxDepth);
            if (trick.player() == aiPos){
                bestEval = Math.max(eval, bestEval);
                alpha = Math.max(bestEval, alpha);
            }else {
                bestEval = Math.min(eval, bestEval);
                beta = Math.min(bestEval, beta);
            }
            if (beta <= alpha) return bestEval;
        }
        if (!trick.talonGeschlossen() && trick.isLeading()){
            Trick copy = trick.copy();
            copy.talonsperre();
            int eval = miniMax(copy, aiPos, alpha, beta, depth + 1, maxDepth);
            if (trick.player() == aiPos){
                bestEval = Math.max(eval, bestEval);
                alpha = Math.max(bestEval, alpha);
            }else {
                bestEval = Math.min(eval, bestEval);
                beta = Math.min(bestEval, beta);
            }
            if (beta <= alpha) return bestEval;
        }
        return bestEval;
    }
}
