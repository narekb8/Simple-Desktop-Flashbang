package com.example;

import java.util.*;
import java.util.Timer;
import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.*;
import com.github.twitch4j.helix.domain.CustomRewardList;
import com.github.twitch4j.helix.domain.UserList;
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent;

/**
 * The main class of the Flashbang App
 * Refer to the README on how to use the program as an end-user
 * @param size final Dimension variable to hold the dimensions of the screen 
 * @param w,h final ints used to store the individual width and height values respectively
 * @param can Canvas that will be used to paint the flashbang JFrame white
 * @param f,g JFrame variables that will be used to draw the flashbang and the control gui respectively
 * @param textField1 JTextField that is used to get user input for their OAuthToken
 * @param total final float meant to be the time counter for the after image fade loop
 * @param period final float for the rate at which the image will fade, the ratio of total/time is how many repetitions until it is faded
 * @param runCount int for the loop that ticks the counter for the fade of the after image
 * @param flashToggle boolean to note if a flash is currently active, used in case a second flash was queued during one so they layer properly
 * @param buttonPressed boolean to note if the random rate button was previously pressed, used to let the button toggle
 * @param afterImage a JComponent that holds the transparent after image once it has been processed
 * @param tillNext double that holds the time (in minutes) until the next random flashbang
 * @param timer Timer class to store the various timers used
 * @param rand Random class to randomly generate the time until the next flashbang
 * @param OAuthToken String to hold the end-users OAuthToken
 * @param channelID String to hold the channelID of the end-users Twitch channel
 * @param rewardList List type provided by the Twitch API to hold a list of custom Channel Point Redemptions
 * @param flashbangID String to hold the Channel Point Redemption ID for the the Redemption to listen to
 * @param fileCreated boolean to note if a properties file had to be created or not
 */

public class App {

    public static final Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
    public static final int w = (int)size.getWidth();
    public static final int h = (int)size.getHeight();

    public static Canvas can = new Canvas(w, h);
    public static JFrame f = new JFrame();
    public static JFrame g = new JFrame();
    public static JTextField textField1 = new JTextField(25);

    public static final float total = 2990;
    public static final float period = 3;
    public static int runCount = 0;
    public static boolean flashToggle = false;
    public static boolean buttonPressed = false;
    public static JComponent afterImage;

    public static double tillNext = 0;

    public static Timer timer = new Timer();
    public static Random rand = new Random();

    public static String OAuthToken = "";
    public static String channelID = "";
    public static CustomRewardList rewardList;
    public static String flashbangID = "";
    public static boolean fileCreated = false;

    public static void main(String[] args) throws IOException
    {
        // Create new all of the requried JFrame Components

        JPanel twitchPanel = new JPanel();
        JPanel localPanel = new JPanel();
        JLabel label = new JLabel();
        JButton button = new JButton();
        
        Container pane = g.getContentPane();
        
        // Create Local Appdata directory to store OAuth Token

        Files.createDirectories(Paths.get(System.getenv("APPDATA") + "\\Flashbang"));
        File fi = new File(System.getenv("APPDATA") + "\\Flashbang\\properties.txt");
        fileCreated = fi.createNewFile();
        if(!fileCreated)
        {
            FileReader file = new FileReader(fi);
            BufferedReader in = new BufferedReader(file);
            OAuthToken = in.readLine();
            if(OAuthToken != "")
            {
                textField1.setText(OAuthToken);
                fileCreated = true;
            }
            in.close();
        }

        // Establish layouts and add JComponents to the proper panels 

        pane.setLayout(new BorderLayout());
        twitchPanel.setLayout(new FlowLayout());
        localPanel.setLayout(new FlowLayout());

        twitchPanel.add(label);
        twitchPanel.add(textField1);

        localPanel.add(button);

        // OAuth Dev Credentials

        OAuth2Credential credentials = new OAuth2Credential
            ("twitch", "hl8584yn6jxdmrk618813xgymzq5ty");
        
        TwitchClient twitchClient = TwitchClientBuilder.builder()
            .withClientId("4ocquuv1wpfordfq04za5l7anqch0e")
            .withClientSecret("iewkws4xqp7anvnzfv5150pddysvx8")
            .withEnableHelix(true)
            .withEnablePubSub(true)
            .build();
        
        // UI, Type of Use, and User Authorization

        label.setText("Enter your OAuth Token, then press Enter to start: ");
        button.setText("Random Timer Only");

        button.addActionListener(new ActionListener() 
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                // Check if the button was previously pressed by the user
                // If yes, cancel all operations
                // If no, start the flash process
                if(!buttonPressed)
                {
                    firstLaunch(false);
                    button.setText("Cancel Flashbangs");
                }
                else
                {
                    flashToggle = false;
                    buttonPressed = false;
                    timer.cancel();
                    f.dispose();
                    f.setOpacity(1);
                    button.setText("Random Timer Only");
                }
            }
        });

        textField1.addActionListener(new ActionListener()
        {
            // Upon confirming the textbox, save the text into the user OAuthToken variable
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e)
            {
                String temp = OAuthToken;
                OAuthToken = textField1.getText();
                
                // If a local file was created for the first time, or if the OAuth token is different, update the file with the last used OAuthToken
                if(fileCreated)
                {
                    try
                    {
                        FileWriter file = new FileWriter(fi);
                        BufferedWriter out = new BufferedWriter(file);
                        out.write(OAuthToken, 0, OAuthToken.length());
                        out.close();
                    }
                    catch (Exception exc)
                    {
                        System.out.println("L Writer");
                    }
                }
                else if(!temp.equals(OAuthToken))
                {
                    try
                    {
                        FileWriter file = new FileWriter(fi);
                        new PrintWriter(fi).close();
                        BufferedWriter out = new BufferedWriter(file);
                        out.write(OAuthToken, 0, OAuthToken.length());
                        out.close();
                    }
                    catch (Exception exc)
                    {
                        System.out.println("L Writer");
                    }
                }

                // Use the user-provided OAuth Token to connect as a Helix client and grab the user, then grab the channel ID
                // From there, grab the list of channel points rewards and listen for the first redemption that uses the word "Flashbang"
                UserList resultList = twitchClient.getHelix().getUsers(OAuthToken, null, null).execute();
                channelID = resultList.getUsers().get(0).getId();
                twitchClient.getPubSub().listenForChannelPointsRedemptionEvents(credentials, channelID);
                rewardList = twitchClient.getHelix().getCustomRewards(OAuthToken, channelID, null, null).execute();
                rewardList.getRewards().forEach(reward -> 
                {
                    if(reward.getTitle().toLowerCase().contains("flashbang"))
                        flashbangID = reward.getId();
                });

                // On redemption of the previously marked channel event, start the flashbang process
                twitchClient.getEventManager().onEvent(RewardRedeemedEvent.class, new FlashbangConsumer<RewardRedeemedEvent>());

                button.setText("Cancel Flashbangs");
                firstLaunch(true);
            }
        });

        // Set proper sizes for all of the control panel JPanels and set other important data, and make the control panel
        localPanel.setSize(new Dimension((int)button.getPreferredSize().getWidth()*2, (int)button.getPreferredSize().getHeight()*2));
        twitchPanel.setSize(new Dimension((int)button.getPreferredSize().getWidth()*10, (int)button.getPreferredSize().getHeight()*2));
        g.setTitle("Flashbang Config");
        g.setBackground(Color.LIGHT_GRAY);
        pane.add(twitchPanel, BorderLayout.NORTH);
        pane.add(localPanel, BorderLayout.SOUTH);
        g.pack();
        g.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        g.setVisible(true);
    }

    // Small function used to play the sound and begin the Timer for the after image fade-out

    public static void startFlash()
    {
        playSound("flashbang.wav");
        timer.scheduleAtFixedRate(new Helper(), 1880, 3);
    }

    // Modified version of startFlash() that calls a different variation of the Helper class that does not start a loop

    public static void startFlashTwitch()
    {
        playSound("flashbang.wav");
        timer.scheduleAtFixedRate(new HelperTwitch(), 1880, 3);
    }

    // Function for the first flashbang effect

    public static void firstLaunch(boolean isTwitch)
    {
        // Will generate random time until next flash using a normal distribution
        // Uses a mean of 7.5 minutes and standard deviation of 3
        tillNext = rand.nextGaussian()*3+7.5;

        // Dispose of any potential leftover objects
        timer.cancel();
        f.dispose();

        // Setup the JFrame that will appear as the "Flashbang"
        f.setOpacity(1);
        f.setSize(size);
        f.setTitle("Flashbang");
        f.add(can);
        f.setAlwaysOnTop(true);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setExtendedState(JFrame.MAXIMIZED_BOTH); 
        f.setUndecorated(true);

        // Screenshot the current desktop and make it transparent so that it can fade out
        Rectangle capture = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
        try
        {
            Robot r = new Robot();
            BufferedImage Image = r.createScreenCapture(capture);
            afterImage = new TransparentImage(Image);
        }
        catch (AWTException e1)
        {
            System.out.println(e1);
        }

        // Add the image to the JFrame and prepare it to flash
        f.add(afterImage);
        afterImage.setOpaque(false);
        afterImage.setVisible(false);
        f.setVisible(true);
        flashToggle = true;
        buttonPressed = true;
        timer = new Timer();

        // Flash once. If non twitch integrated begin the random loop process.
        if(isTwitch)
            startFlashTwitch();
        else
            startFlash();
    }

    // Function to play the sound effect

    public static synchronized void playSound(String soundFile) {
        try {
            Clip clip = AudioSystem.getClip();
            URL url = App.class.getResource("audio/" + soundFile);
            AudioInputStream inputStream = AudioSystem.getAudioInputStream(url);
            clip.open(inputStream);
            clip.start();
        } catch (Exception e) {
            System.out.println("L");
        }
    }

    // Helper class for the Timer of the local loop variant of the program
    // Will fade the after image after the set time and call the next run

    public static class Helper extends TimerTask
    {
        public void run()
        {
            if(flashToggle)
            {
                if(runCount == (int)total/(int)period)
                {
                    f.setOpacity(0);
                    afterImage.setVisible(false);
                    timer.cancel();
                    runCount = 0;
                    f.setVisible(false);
                    flashToggle = false;
                    System.out.println(tillNext);
                    timer = new Timer();
                    timer.schedule(new Rerun(), (long)(tillNext * 60000));
                }
                else
                {
                    afterImage.setVisible(true);
                    f.setOpacity(f.getOpacity() - (period / total));
                    runCount++;
                }
            }
        }
    }

    // Helper class for the Timer of the Twitch integrated version of the program
    // Will fade the after image after the set time

    public static class HelperTwitch extends TimerTask
    {
        public void run()
        {
            if(flashToggle)
            {
                if(runCount == (int)total/(int)period)
                {
                    f.setOpacity(0);
                    afterImage.setVisible(false);
                    timer.cancel();
                    runCount = 0;
                    f.setVisible(false);
                    flashToggle = false;
                }
                else
                {
                    afterImage.setVisible(true);
                    f.setOpacity(f.getOpacity() - (period / total));
                    runCount++;
                }
            }
        }
    }

    // Private helper class for the Timer that, for the local loop version of the program, handles the activation of the next flashbang
    // Is essentially a cut-down version of startFlash()

    private static class Rerun extends TimerTask
    {
        public void run()
        {
            tillNext = rand.nextGaussian()*3+7.5;
            f.toFront();
            f.remove(afterImage);

            Rectangle capture = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            try
            {
                Robot r = new Robot();
                BufferedImage Image = r.createScreenCapture(capture);
                afterImage = new TransparentImage(Image);
            }
            catch (AWTException e1)
            {
                System.out.println(e1);
            }

            playSound("flashbang.wav");
            f.add(afterImage);
            afterImage.setOpaque(false);
            afterImage.setVisible(false);
            f.setOpacity(1);
            f.setVisible(true);
            flashToggle = true;
            timer.scheduleAtFixedRate(new Helper(), 1880, 3);
        }
    }
}