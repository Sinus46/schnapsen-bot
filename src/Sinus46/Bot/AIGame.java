package Sinus46.Bot;

import Sinus46.AI.Tree;
import Sinus46.Schnapsen.Card;
import Sinus46.Schnapsen.Trick;
import Sinus46.ThreadLocker;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class AIGame extends ListenerAdapter {
    private final User gamer;
    private final JDA jda;
    private final int lobbyID;
    private final MessageChannel channel;
    private final ResourceBundle resources;
    private final boolean german;
    private Trick trick;
    private final int[] scores = {7, 7};
    private Message msg;
    private final String uuid = UUID.randomUUID().toString();
    private int vorhand = 0;
    private boolean ansage = false;

    public AIGame(User gamer, JDA jda, int lobbyID, MessageChannel channel, ResourceBundle resources, boolean german){
        this.gamer = gamer;
        this.jda = jda;
        this.lobbyID = lobbyID;
        this.channel = channel;
        this.resources = resources;
        this.german = german;
        loop();
    }

    private void loop(){
        new Thread(() -> {
            do {
                trick = new Trick(german);
                while (trick.ergebnis() == 0) {
                    if (trick.player() == vorhand){
                        sendMessages();
                        ThreadLocker.pause();
                    }else{
                        sendMessages();
                        Integer best = Tree.normalAI(trick, scores, 3e+9).entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue)).get().getKey();
                        if (best >= 0) {
                            Card bestCard = trick.getHand(trick.player()).content().get(best);
                            ansage = trick.getHand(trick.player()).isPaired(bestCard) && trick.isLeading();
                        }
                        switch (best){
                            case -2 -> trick.talonsperre();
                            case -1-> trick.exchange();
                            default -> trick.play(best);
                        }
                    }
                }
                scores[(trick.player() + vorhand) % 2] -= trick.ergebnis();
                vorhand = (vorhand + 1) % 2;
                if (!(scores[0] < 0 || scores[1] < 0)) {
                    try {
                        zwischenNachrichten();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } while (!(scores[0] <= 0 || scores[1] <= 0));
            gameEndMessages();
            jda.removeEventListener(this);
        }).start();
    }

    private MessageEmbed generateEmbed(){
        int j = (vorhand) % 2;
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(resources.getString("schnapsen")).setColor(switch (trick.talonSize()){
            case 0 -> 0xbd1c00;
            case -1 -> 0x871400;
            default -> 0x0286d9;
        });
        builder.addField(resources.getString("trump"),
                        trick.talonGeschlossen() ? Card.parseSuit(trick.getTrump().suit(), german) + "" : trick.getTrump().toString(),
                        true)
                .addField(resources.getString("talon"), switch (trick.talonSize()){
                    case 0 -> resources.getString("closed");
                    case -1 -> resources.getString("locked");
                    default -> resources.getString("remaining").replaceAll("\\$s",
                            String.valueOf(trick.talonSize()));
                }, true);
        builder.addField(resources.getString("points"), trick.scores()[j] + "", true);
        builder.addField(resources.getString("trick"), trick + "",false);
        builder.addField(resources.getString("hand"), trick.getHand(j) + "", false);
        builder.setFooter(gamer.getName() + " " + scores[0] + " - " + scores[1] + " " + resources.getString("ai"));
        return builder.build();
    }
    private List<ActionRow> buttons(){
        int j = (vorhand) % 2;
        List<Button> row1 = new ArrayList<>();
        List<ActionRow> ausgabe = new ArrayList<>();
        List<Card> content = trick.getHand(j).content();
        for (int i = 0; i < content.size(); i++) {
            Card card = content.get(i);
            Button button = Button.secondary(uuid + ":" + i , card + "");
            if (card.suit() == trick.getTrump().suit()) button = button.withStyle(ButtonStyle.PRIMARY);
            if (trick.getHand(j).isPaired(card)) button = button.withStyle(ButtonStyle.SUCCESS);
            if (!trick.getHand(j).isPlayable(card)) button = button.asDisabled();
            row1.add(button);
        }
        ausgabe.add(ActionRow.of(row1));
        if (!trick.isLeading()) return ausgabe;
        List<Button> row2 = new ArrayList<>();
        if (trick.getHand(j).canExchange()) row2.add(Button.primary(uuid + ":austausch", resources.getString("exchange")));
        if (!trick.talonGeschlossen()) row2.add(Button.danger(uuid + ":sperre", resources.getString("lock")));
        if (!row2.isEmpty()) ausgabe.add(ActionRow.of(row2));
        return ausgabe;
    }
    private void sendMessages(){
        if (msg != null) msg.delete().queue();
        PrivateChannel channel = gamer.openPrivateChannel().complete();
        MessageBuilder msg = new MessageBuilder();
        if (ansage) msg.setContent(resources.getString("meld"));
        msg.setEmbeds(generateEmbed());
        if (vorhand == trick.amStich()) msg.setActionRows(buttons());
        this.msg = channel.sendMessage(msg.build()).complete();
    }

    private MessageEmbed gameEndEmbed(){
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(resources.getString("last_trick")).setColor(0x671e73);
        builder.addField(resources.getString("you"), gamer.getName() + " " + (Math.max(scores[0], 0)), true);
        builder.addField(resources.getString("your_opponent"), resources.getString("ai") + " " + Math.max(scores[1], 0), true);
        builder.addField(resources.getString("last_trick"), trick + "", false);
        return builder.build();
    }
    private void zwischenNachrichten() throws InterruptedException {
        gamer.openPrivateChannel().complete().sendMessageEmbeds(gameEndEmbed()).queue();
        Thread.sleep(1000);
    }
    private void gameEndMessages(){
        if (msg != null) msg.delete().queue();
        gamer.openPrivateChannel().complete().sendMessageEmbeds(gameEndEmbed()).queue();
        channel.sendMessageEmbeds(gameEndEmbed()).queue();
    }

    @Override
    public void onButtonClick(@NotNull ButtonClickEvent event) {
        String[] id = event.getComponentId().split(":");
        if (!id[0].equals(uuid)) return;
        switch (id[1]){
            case "sperre" -> trick.talonsperre();
            case "austausch" -> trick.exchange();
            default -> {
                ansage = (event.getButton().getStyle() == ButtonStyle.SUCCESS && trick.isLeading());
                trick.play(Integer.parseInt(id[1]));
            }
        }
        ThreadLocker.resume();
    }
}
