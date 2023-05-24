package com.example;

import java.util.*;
import java.util.Timer;
import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.github.philippheuer.credentialmanager.domain.OAuth2Credential;
import com.github.twitch4j.*;
import com.github.twitch4j.chat.events.channel.CheerEvent;
import com.github.twitch4j.chat.events.channel.SubscriptionEvent;
import com.github.twitch4j.helix.domain.CustomRewardList;
import com.github.twitch4j.helix.domain.UserList;
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent;


/**
 * The main class of the Flashbang App
 * Refer to the README on how to use the program as an end-user
 * @param size Dimension variable to hold the dimensions of the screen 
 * @param w,h ints used to store the individual width and height values respectively
 * @param can Canvas that will be used to paint the flashbang JFrame white
 * @param f,g JFrame variables that will be used to draw the flashbang and the control gui respectively
 * @param monitorList JComboBox of monitor keys, allows user to select desired monitor
 * @param bitsText JTextField for user to input the minimum number of bits to trigger a flash
 * @param redeemText JTextField for user to input the name of the channel point redemption
 * @param useTwitch JCheckBox to enable or disable Twitch Integration
 * @param bitsEn JCheckBox to enable or disable bits with Twitch Integration
 * @param redeemEn JCheckBox to enable or disable channel point redemptions with Twitch Integration
 * @param subsEn JCheckBox to enable or disable subs with Twitch Integration
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
 * @param channelID String to hold the channel ID of the end-users Twitch channel
 * @param channelName String to hold the channel name of the end-users Twitch channel
 * @param rewardList List type provided by the Twitch API to hold a list of custom Channel Point Redemptions
 * @param flashbangID String to hold the Channel Point Redemption ID for the the Redemption to listen to
 * @param fileCreated boolean to note if a properties file had to be created or not
 * @param isTwitch boolean for the pilot flash function to determine if local or twitch-integrated
 * @param splitter final String that determines the string used by the properties file to split data points
 * @param auth global Authenticator variable for the program
 * @param clientID client ID of the program in Twitch Dev Console
 * @param monMap HashMap of the connected monitors, key is the result of getIDString() on each GraphicsDevice
 */

public class App {

    public static Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
    public static int w = (int)size.getWidth();
    public static int h = (int)size.getHeight();

    public static Canvas can = new Canvas(w, h);
    public static JFrame f = new JFrame();
    public static JFrame g = new JFrame();
    public static JComboBox<String> monitorList;

    public static JTextField bitsText = new JTextField(10);
    public static JTextField redeemText = new JTextField(10);

    public static JCheckBox useTwitch = new JCheckBox("Use Twitch Integration");
    public static JCheckBox bitsEn = new JCheckBox("Bits");
    public static JCheckBox redeemEn = new JCheckBox("Channel Points");
    public static JCheckBox subsEn = new JCheckBox("Sub");

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
    public static String channelName = "";
    public static CustomRewardList rewardList;
    public static String flashbangID = "";
    public static boolean fileCreated = false;
    public static boolean isTwitch = false;
    public static final String splitter = String.format("%c%c", (char)200, (char)201);

    public static Authenticator auth;
    public static String clientID = <DROP_YOUR_CLIENTID_HERE>;
    public static Map<String, GraphicsDevice> monMap;

    public static void main(String[] args) throws Exception
    {
        // Create all of the requried local JFrame Components
        JPanel twitchPanel = new JPanel();
        JPanel twitchToken = new JPanel();
        JPanel twitchOpt = new JPanel();
        JPanel twitchSettings = new JPanel();
        JPanel localPanel = new JPanel();

        JPanel bitsOpt = new JPanel();
        JPanel redeemOpt = new JPanel();

        JLabel bitsField = new JLabel("Minimum Bits Required: ");
        JLabel redeemField = new JLabel("Point Redemption Name: ");

        JLabel label = new JLabel("Paste OAuthToken for Twitch, leave blank for local: ");
        JButton button = new JButton("Flashes On");
        JButton authButton = new JButton("Connect to Twitch");

        monMap = new HashMap<String, GraphicsDevice>();

        // Grab the list of all connected Graphics Devices we can output to, and store them in a HashMap
        for(GraphicsDevice i : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices())
        {
            monMap.put(i.getIDstring(), i);
        }

        // Create a temporary array to store the keys of the HashMap, then use it to generate the ComboBox
        String[] strings = new String[monMap.keySet().size()];
        monitorList = new JComboBox<String>(monMap.keySet().toArray(strings));

        Container pane = g.getContentPane();
        
        // Create Local Appdata directory to store Local Data

        Files.createDirectories(Paths.get(System.getenv("APPDATA") + "\\Flashbang"));
        File fi = new File(System.getenv("APPDATA") + "\\Flashbang\\properties.cfg");
        fileCreated = fi.createNewFile();
        
        if(!fileCreated)
        {
            FileReader file = new FileReader(fi);
            BufferedReader in = new BufferedReader(file);
            StringBuilder dataIn = new StringBuilder(in.readLine());
            if(dataIn.capacity() > 0)
            {
                String[] data = dataIn.toString().split(splitter);
                OAuthToken = data[0].substring(0, data[0].length()-1);
                channelName = data[1].substring(0, data[1].length()-1);
                channelID = data[2].substring(0, data[2].length()-1);
                bitsText.setText(data[3].substring(0, data[3].length()-1));
                redeemText.setText(data[4].substring(0, data[4].length()-1));

                useTwitch.setSelected(((char)in.read() == (char)49) ? true : false);
                bitsEn.setSelected(((char)in.read() == (char)49) ? true : false);
                redeemEn.setSelected(((char)in.read() == (char)49) ? true : false);
                subsEn.setSelected(((char)in.read() == (char)49) ? true : false);
                monitorList.setSelectedIndex((in.read())-33);
            }
            in.close();
        }

        // Establish layouts and add JComponents to the proper panels 

        pane.setLayout(new BorderLayout());
        twitchPanel.setLayout(new BorderLayout());
        twitchToken.setLayout(new FlowLayout());
        twitchOpt.setLayout(new FlowLayout());
        twitchSettings.setLayout(new FlowLayout());
        localPanel.setLayout(new FlowLayout());

        twitchToken.add(authButton);
        if(!OAuthToken.isEmpty())
        {
            authButton.setText("Logout User");
            label.setText("Current Connected User: " + channelName);
            twitchToken.add(label);
            twitchToken.add(useTwitch);
        }

        twitchOpt.add(bitsEn);
        twitchOpt.add(redeemEn);
        twitchOpt.add(subsEn);

        bitsOpt.add(bitsField);
        bitsOpt.add(bitsText);

        redeemOpt.add(redeemField);
        redeemOpt.add(redeemText);

        localPanel.add(button);

        twitchPanel.add(twitchToken, BorderLayout.NORTH);
        twitchPanel.add(twitchOpt, BorderLayout.CENTER);
        twitchPanel.add(twitchSettings, BorderLayout.SOUTH);

        // Establish OAuth Credentials and Twitch Client

        OAuth2Credential credentials = new OAuth2Credential
            ("twitch", OAuthToken);
        
        ITwitchClient twitchClient = TwitchClientBuilder.builder()
            .withClientId(clientID)
            .withEnableHelix(true)
            .withEnablePubSub(true)
            .withEnableChat(true)
            .build();
        
        // UI, Type of Use, and User Authorization

        // The next two listeners are just to enable the appropriate text fields for each checkbox

        bitsEn.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == 1)
                    twitchSettings.add(bitsOpt, BorderLayout.SOUTH);
                else
                    twitchSettings.remove(bitsOpt);

                g.revalidate();
                g.repaint();
            }
            
        });

        redeemEn.addItemListener(new ItemListener()
        {
            @Override
            public void itemStateChanged(ItemEvent e) {
                if(e.getStateChange() == 1)
                    twitchSettings.add(redeemOpt, BorderLayout.SOUTH);
                else
                    twitchSettings.remove(redeemOpt);

                g.revalidate();
                g.repaint();
            }
        });

        // Set the appropriate size and dimensions for the selected monitor
        monitorList.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e) {
                GraphicsDevice sel = monMap.get(monitorList.getSelectedItem());
                size = new Dimension(sel.getDisplayMode().getWidth(), sel.getDisplayMode().getHeight());
                w = sel.getDisplayMode().getWidth();
                h = sel.getDisplayMode().getHeight();
            }           
        });

        // Ensure only numbers can be input in the minimum bit text field
        bitsText.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent ke) {
               if ((ke.getKeyChar() >= '0' && ke.getKeyChar() <= '9') || ke.getKeyCode() == 8 || ke.getKeyChar() == 127) {
                  bitsText.setEditable(true);
               } else {
                  bitsText.setEditable(false);
               }
            }
        });

        // Main button to begin flashing
        button.addActionListener(new ActionListener() 
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                // Check if the button was previously pressed by the user
                // If yes, cancel all operations
                // If no, start the flash process, and disable all other UI elements
                if(!buttonPressed)
                {
                    authButton.setEnabled(false);
                    useTwitch.setEnabled(false);
                    bitsEn.setEnabled(false);
                    redeemEn.setEnabled(false);
                    subsEn.setEnabled(false);
                    bitsText.setEnabled(false);
                    redeemText.setEnabled(false);
                    monitorList.setEnabled(false);

                    if(!useTwitch.isSelected())
                    {
                        firstLaunch(false);
                        button.setText("Cancel Flashbangs");
                    }
                    else
                    {
                        // Use the user-provided OAuth Token to connect as an IRC client and listen into the user's chat
                        // From there, check which options are selected for Twitch compatability, and build the related listeners for each
                        twitchClient.getChat().joinChannel(channelName);

                        if(bitsEn.isSelected())
                            twitchClient.getEventManager().onEvent(CheerEvent.class, new FlashbangBitConsumer<CheerEvent>());

                        if(subsEn.isSelected())
                            twitchClient.getEventManager().onEvent(SubscriptionEvent.class, new FlashbangSubConsumer<SubscriptionEvent>());

                        if(redeemEn.isSelected())
                        {
                            twitchClient.getPubSub().listenForChannelPointsRedemptionEvents(credentials, channelID);

                            rewardList = twitchClient.getHelix().getCustomRewards(OAuthToken, channelID, null, null).execute();
                            rewardList.getRewards().forEach(reward -> 
                            {
                                if(reward.getTitle().toLowerCase().contains(redeemText.getText().toLowerCase()))
                                    flashbangID = reward.getId();
                            });
                            System.out.println(flashbangID);

                            twitchClient.getEventManager().onEvent(RewardRedeemedEvent.class, new FlashbangConsumer<RewardRedeemedEvent>());
                        }

                        button.setText("Cancel Flashbangs");
                        firstLaunch(true);
                    }
                    
                }
                else
                {
                    authButton.setEnabled(true);
                    useTwitch.setEnabled(true);
                    bitsEn.setEnabled(true);
                    redeemEn.setEnabled(true);
                    subsEn.setEnabled(true);
                    bitsText.setEnabled(true);
                    redeemText.setEnabled(true);
                    monitorList.setEnabled(true);

                    flashToggle = false;
                    buttonPressed = false;
                    timer.cancel();
                    f.dispose();
                    f.setOpacity(1);
                    button.setText("Flashes On");
                }
            }
        });

        // Button to connect or disconnect Twitch account from program
        // When successfully authorized, store the token and edit UI elements appropriately
        authButton.addActionListener(new ActionListener() 
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if(OAuthToken.equals(""))
                {
                    try {
                        if(authUser())
                        {
                            credentials.setAccessToken(OAuthToken);
    
                            UserList resultList = twitchClient.getHelix().getUsers(OAuthToken, null, null).execute();
                            channelID = resultList.getUsers().get(0).getId();
                            channelName = resultList.getUsers().get(0).getDisplayName();
                            
                            label.setText("Current Connected User: " + channelName);
                            authButton.setText("Logout User");
                            twitchToken.add(label);
                            twitchToken.add(useTwitch);
    
                            g.revalidate();
                            g.repaint();
                        }
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
                else
                {
                    OAuthToken = channelID = channelName = "";
                    credentials.setAccessToken("");
                    twitchToken.remove(label);
                    twitchToken.remove(useTwitch);
                    authButton.setText("Connect to Twitch");

                    g.revalidate();
                    g.repaint();
                }
            }
        });

        // Set proper sizes for all of the control panel JPanels and set other important data, and make the control panel
        localPanel.setSize(new Dimension((int)button.getPreferredSize().getWidth()*2, (int)button.getPreferredSize().getHeight()*2));
        twitchPanel.setSize(new Dimension((int)button.getPreferredSize().getWidth()*10, (int)button.getPreferredSize().getHeight()*2));
        g.setTitle("Flashbang Config");
        g.setBackground(Color.LIGHT_GRAY);
        twitchSettings.add(bitsOpt);
        twitchSettings.add(redeemOpt);
        pane.add(twitchPanel, BorderLayout.NORTH);
        pane.add(localPanel, BorderLayout.CENTER);
        pane.add(monitorList, BorderLayout.SOUTH);
        g.pack();

        if(!bitsEn.isSelected())
            twitchSettings.remove(bitsOpt);
        if(!redeemEn.isSelected())
            twitchSettings.remove(redeemOpt);

        g.addWindowListener(new CloseFrame());
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
        f.setLocation(
            monMap.get(monitorList.getSelectedItem())
            .getDefaultConfiguration().getBounds().x,
            monMap.get(monitorList.getSelectedItem())
            .getDefaultConfiguration().getBounds().y + f.getY());
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

    // Function for getting an Access Token from the Twitch API, adapted scripts
    // from the Java Twitch Api Wrapper by Matthew Bell, github repo for his wrapper
    // of the old Twitch Api can be found at https://github.com/urgrue/Java-Twitch-Api-Wrapper

    private static boolean authUser() throws URISyntaxException, IOException
    {
        URI callbackUri = new URI("http://localhost:3000/authorize.html");
        auth = new Authenticator("https://id.twitch.tv");
        String authUrl = auth.getAuthenticationUrl(clientID, callbackUri, Scopes.CHANNEL_READ_REDEMPTIONS);
        
        Desktop.getDesktop().browse(new URI(authUrl));
        boolean authSuccess = auth.awaitAccessToken();

        if(authSuccess)
        {    
            OAuthToken = auth.getAccessToken();
            isTwitch = true;
            return true;
        }
        else
        {
            System.out.println(auth.getAuthenticationError());
            return false;
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
            f.setLocation(
                monMap.get(monitorList.getSelectedItem())
                .getDefaultConfiguration().getBounds().x,
                monMap.get(monitorList.getSelectedItem())
                .getDefaultConfiguration().getBounds().y + f.getY());
            f.setVisible(true);
            flashToggle = true;
            timer.scheduleAtFixedRate(new Helper(), 1880, 3);
        }
    }

    private static class CloseFrame extends WindowAdapter
    {
        public void windowClosing(WindowEvent evt)
        {
            try {
                closeHandler();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        private void closeHandler() throws IOException
        {
            Files.createDirectories(Paths.get(System.getenv("APPDATA") + "\\Flashbang"));
            File fi = new File(System.getenv("APPDATA") + "\\Flashbang\\properties.cfg");
            fileCreated = fi.createNewFile();

            FileWriter file = new FileWriter(fi);
            BufferedWriter out = new BufferedWriter(file);

            StringBuilder dataString = new StringBuilder(OAuthToken + "q");
            dataString.append(splitter);
            dataString.append(channelName + "q");
            dataString.append(splitter);
            dataString.append(channelID + "q");
            dataString.append(splitter);
            dataString.append(bitsText.getText() + "q");
            dataString.append(splitter);
            dataString.append(redeemText.getText() + "q");

            out.write(dataString.toString());
            out.newLine();

            dataString = new StringBuilder(4);
            dataString.append((useTwitch.isSelected()) ? "1" : "0");
            dataString.append((bitsEn.isSelected()) ? "1" : "0");
            dataString.append((redeemEn.isSelected()) ? "1" : "0");
            dataString.append((subsEn.isSelected()) ? "1" : "0");
            dataString.append((char)(monitorList.getSelectedIndex()+33));

            out.write(dataString.toString());
            out.close();
        }
    }
}