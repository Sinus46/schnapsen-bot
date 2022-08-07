package Sinus46.Schnapsen;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Trick {
    private Card stich = null;
    private Card[] geschichte;
    private int playing = 0;
    private Card trump;
    private final List<Card> deck;
    private final Hand[] hands;
    private boolean talonGeschlossen = false;
    private int ergebnis = 0;
    private int talonSperrer = 0;

    public Trick(boolean german){
        deck = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            deck.add(Card.fromInt(i, german));
        }
        Collections.shuffle(deck);
        hands = new Hand[]{new Hand(new ArrayList<>(deck.subList(0, 5))), new Hand(new ArrayList<>(deck.subList(5, 10)))};
        deck.removeAll(deck.subList(0, 10));
        trump = deck.get(deck.size() - 1);
    }

    public Trick() {
        this(true);
    }

    private Trick(Card stich, Card[] geschichte, int playing, Card trump,
            List<Card> deck, Hand[] hands, boolean talonGeschlossen, int ergebnis, int talonSperrer){
        this.stich = stich;
        this.geschichte = geschichte;
        this.playing = playing;
        this.trump = trump;
        this.deck = new ArrayList<>(deck);
        this.hands = new Hand[]{new Hand(hands[0].content, hands[0].score, hands[0].viennaScore),
                new Hand(hands[1].content, hands[1].score, hands[1].viennaScore)};
        this.talonGeschlossen = talonGeschlossen;
        this.ergebnis = ergebnis;
        this.talonSperrer = talonSperrer;
    }

    public Trick copy(){
        return new Trick(stich,
                geschichte,
                playing,
                trump,
                deck,
                hands,
                talonGeschlossen,
                ergebnis,
                talonSperrer);
    }

    public Trick randomizedCopy(int perspective, boolean randomizeHand){
        if (deck.size() == 0) return this.copy();
        List<Card> unknown = new ArrayList<>(deck.subList(0, deck.size() - 1));
        if (randomizeHand) {
            unknown.addAll(hands[1 - perspective].content);
        }
        Collections.shuffle(unknown);
        Hand[] newHands = new Hand[2];
        if (randomizeHand){
            Hand newHand = new Hand(new ArrayList<>(unknown.subList(0, hands[1 - perspective].content.size())));
            newHand.score = hands[1-perspective].score;
            newHand.viennaScore = hands[1-perspective].viennaScore;
            unknown.removeAll(newHand.content);
            newHands[1-perspective] = newHand;
        } else {
            newHands[1-perspective] = hands[1-perspective];
        }
        List<Card> newDeck = new ArrayList<>(unknown);
        newDeck.add(trump);
        newHands[perspective] = hands[perspective];
        return new Trick(stich,
                geschichte,
                playing,
                trump,
                newDeck,
                newHands,
                talonGeschlossen,
                ergebnis,
                talonSperrer);
    }

    public void exchange(){
        if (ergebnis != 0) return;
        if (stich != null) throw new RuntimeException();
        Card jack = new Card(trump.suit(), 0, trump.german());
        if (!hands[playing].content.remove(jack)) throw new IllegalArgumentException("Player can't exchange.");
        hands[playing].content.add(trump);
        trump = jack;
        deck.remove(deck.size() - 1);
        deck.add(jack);
    }

    public void play(int index){
        if (ergebnis != 0) return;
        Card card = hands[playing].content.remove(index);
        playCard(card);
    }

    public void play(Card card){
        if (ergebnis != 0) return;
        if (!hands[playing].content.remove(card)){
            throw new IllegalArgumentException("Illegal Card");
        }
        playCard(card);
    }

    private void playCard(Card card) {
        if (stich == null){
            stich = card;
            if (hands[playing].isPaired(card)) {
                hands[playing].score += card.suit() == trump.suit() ? 40 : 20;
                victoryCheck();
                if (ergebnis != 0) return;
            }
            playing = 1 - playing;
        }else{
            int value = 0;
            if (card.suit() == stich.suit()){
                value = Card.valueOf(card);
            }else if (card.suit() == trump.suit()){
                value = Card.valueOf(card) + 1000;
            }
            if (Card.valueOf(stich) >= value){
                playing = 1 - playing;
            }
            hands[playing].score += Card.valueOf(card, stich);
            victoryCheck();
            geschichte = new Card[]{stich, card};
            stich = null;
            for (int i = 0; i < 2; i++) {
                Hand hand = hands[(i+playing)%2];
                if (!talonGeschlossen) hand.content.add(deck.remove(0));
                if (deck.size() == 0) talonGeschlossen = true;
            }
        }
    }

    private void victoryCheck() {
        if (talonGesperrt()) {
            if (hands[playing].score >= 66){
                ergebnis = parseScore(hands[1-playing].viennaScore);
                if (talonSperrer != playing){
                    if (ergebnis < 2) ergebnis = 2;
                }
            }else if (hands[playing].content.size() == 0){
                playing = 1 - talonSperrer;
                ergebnis = parseScore(hands[1-playing].viennaScore);
                if (ergebnis < 2) ergebnis = 2;
            }
        }else {
            if (hands[playing].score >= 66 || hands[playing].content.size() == 0) {
                ergebnis = parseScore(hands[1 - playing].score);
            }
        }
    }

    private int parseScore(int score) {
        if (score >= 33) return 1;
        else if (score > 0) return 2;
        else return 3;
    }

    public void talonsperre(){
        if (ergebnis != 0) return;
        if (stich != null) throw new RuntimeException();
        talonGeschlossen = true;
        for (Hand player:hands) {
            player.viennaScore = player.score;
        }
        talonSperrer = playing;
    }

    public Card getTrump() {
        return trump;
    }

    public int talonSize(){
        if (talonGeschlossen && deck.size() != 0) return -1;
        return deck.size();
    }

    public boolean talonGeschlossen(){
        return talonGeschlossen;
    }

    public boolean isLeading(){
        return stich == null;
    }

    public int amStich(){
        return playing;
    }

    public Hand getHand(int index) {
        return hands[index];
    }

    public int ergebnis(){
        return ergebnis;
    }

    public int player(){
        return playing;
    }

    private boolean talonGesperrt(){
        return talonGeschlossen && deck.size() > 0;
    }

    public int[] scores(){
        return new int[]{hands[0].score, hands[1].score};
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if (stich != null) {
            builder.append(stich).append("-");
            return builder.toString();
        }
        if (geschichte != null){
            for (Card card:geschichte) {
                builder.append(card);
            }
            return builder.toString();
        }
        return "--";
    }

    public class Hand {
        private final List<Card> content;
        private int score = 0;
        private int viennaScore = 0;

        public List<Card> content() {
            return content;
        }

        private Hand(List<Card> content, int score, int viennaScore) {
            this.content = content.stream()
                    .sorted(Card.CARD_COMPARATOR).collect(Collectors.toCollection(() -> new ArrayList<>() {
                        @Override
                        public boolean add(Card card) {
                            boolean temp = super.add(card);
                            this.sort(Card.CARD_COMPARATOR);
                            return temp;
                        }
                    }));
            this.score = score;
            this.viennaScore = viennaScore;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            for (Card card:content) {
                builder.append(card).append("  ");
            }
            return builder.toString();
        }

        private Hand(List<Card> content){
            this.content = content.stream()
                    .sorted(Card.CARD_COMPARATOR).collect(Collectors.toCollection(() -> new ArrayList<>() {
                        @Override
                        public boolean add(Card card) {
                            boolean temp = super.add(card);
                            this.sort(Card.CARD_COMPARATOR);
                            return temp;
                        }
                    }));
        }

        public boolean isPaired(Card card){
            return pairOf(card) != null;
        }

        @Nullable
        public Card pairOf(Card card) {
            if (card.index() != 1 && card.index() != 2) return null;
            for (Card partner:content) {
                if (partner.suit() == card.suit()){
                    if (card.index() == 1 && partner.index() == 2) return partner;
                    if (card.index() == 2 && partner.index() == 1) return partner;
                }
            }
            return null;
        }

        public boolean canExchange(){
            if (talonGeschlossen) return false;
            if (stich != null) return false;
            for (Card card : content) {
                if (card.equals(new Card(trump.suit(), 0, trump.german()))) return true;
            }
            return false;
        }

        public List<Card> playableCards(){
            if (!talonGeschlossen || stich == null) return content;
            List<Card> aus = new ArrayList<>();
            for (Card card:content) {
                if (card.suit() == stich.suit() && card.index() > stich.index()) aus.add(card);
            }
            if (!aus.isEmpty()) return aus;
            for (Card card:content) {
                if (card.suit() == stich.suit()) aus.add(card);
            }
            if (!aus.isEmpty()) return aus;
            for (Card card:content) {
                if (card.suit() == trump.suit()) aus.add(card);
            }
            if (!aus.isEmpty()) return aus;
            return content;
        }

        public boolean isPlayable(Card card){
            return playableCards().contains(card);
        }
    }
}
