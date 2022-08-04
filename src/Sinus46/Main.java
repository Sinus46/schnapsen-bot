package Sinus46;

import Sinus46.Bot.Commands;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.security.auth.login.LoginException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws LoginException, InterruptedException {
        System.out.println("Created by Sinus46");
        JDA jda = JDABuilder.create(args[0], getIntents()).build().awaitReady();
        Commands.initCommands(jda);
        Scanner commandLine = new Scanner(System.in);
        while (true) {
            if (commandLine.next().equals("stop")){
                System.out.println("Shutting down...");
                jda.shutdownNow();
                return;
            }
        }
    }
    private static Collection<GatewayIntent> getIntents(){
        return new ArrayList<>(Arrays.asList(GatewayIntent.values()));
    }
}
