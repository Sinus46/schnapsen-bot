package Sinus46.Schnapsen;

import java.io.Serializable;
import java.util.Comparator;

public record Card(int suit, int index, boolean german){

    public static String parseSuit(int suit, boolean german){
        if (german) {
            return switch (suit) {
                case 0 -> "\ud83c\udf43";
                case 1 -> "❤";
                case 2 -> "\ud83c\udf30";
                case 3 -> "\ud83d\udd14";
                default -> throw new IllegalArgumentException("Illegal Suit");
            };
        } else {
            return switch (suit) {
                case 0 -> "♠";
                case 1 -> "♥";
                case 2 -> "♣";
                case 3 -> "♦";
                default -> throw new IllegalArgumentException("Illegal Suit");
            };
        }
    }
    public static String parseIndex(int index, boolean german){
        if (german) {
            return switch (index) {
                case 0 -> "U";
                case 1 -> "O";
                case 2 -> "K";
                case 3 -> "X";
                case 4 -> "D";
                default -> throw new IllegalArgumentException("Illegal Suit");
            };
        } else {
            return switch (index) {
                case 0 -> "J";
                case 1 -> "Q";
                case 2 -> "K";
                case 3 -> "10";
                case 4 -> "A";
                default -> throw new IllegalArgumentException("Illegal Suit");
            };
        }
    }
    public static int valueOf(Card... cards){
        int sum = 0;
        for (Card card:cards) {
            sum += switch(card.index) {
                case 0 -> 2;
                case 1 -> 3;
                case 2 -> 4;
                case 3 -> 10;
                case 4 -> 11;
                default -> throw new IllegalArgumentException("Illegal Card Index");
            };
        }
        return sum;
    }
    public static Card evaluate(String str) {
        String index = str.substring(0, 1);
        if (index.equals("1")) {
            index = str.substring(0, 2);
        }
        String suit = str.replaceAll(index, "");
        return new Card(switch (suit) {
            case "\ud83c\udf43", "♠" -> 0;
            case "❤", "♥" -> 1;
            case "\ud83c\udf30", "♣" -> 2;
            case "\ud83d\udd14", "♦" -> 3;
            default -> -1;
        }, switch (index) {
            case "U", "J" -> 0;
            case "O", "Q" -> 1;
            case "K" -> 2;
            case "X", "10" -> 3;
            case "A", "D" -> 4;
            default -> -1;
        }, switch (suit) {
            case "\ud83c\udf43", "❤", "\ud83c\udf30", "\ud83d\udd14" -> true;
            default -> false;
        });
    }

    @Override
    public String toString() {
        return parseIndex(index, german) + parseSuit(suit, german);
    }

    public static Card fromInt(int from, boolean german){
        return new Card(from / 5, from % 5, german);
    }

    public static final Comparator<Card> CARD_COMPARATOR = (o1, o2) -> (o1.suit - o2.suit) * 100 + o1.index - o2.index;
}
