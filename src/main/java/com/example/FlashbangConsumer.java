package com.example;

import java.util.Timer;
import java.util.function.Consumer;
import java.awt.*;
import java.awt.image.BufferedImage;
import com.github.twitch4j.pubsub.events.RewardRedeemedEvent;

/*
 * Consumer Class for Channel Point Redemptions
 * Performs the same functions as the startFlash() function in the main App class, but slightly cut down
 */

class FlashbangConsumer<E> implements Consumer<RewardRedeemedEvent>
{
    @Override
    public void accept(RewardRedeemedEvent t) {
        if(t.getRedemption().getReward().getId().equals(App.flashbangID))
        {
            App.f.add(App.can);
            App.timer.cancel();
            App.f.setOpacity(1);
            App.f.setTitle("Flashbang");
            App.f.setAlwaysOnTop(true);

            if(!App.flashToggle)
            {
                Rectangle capture = App.monMap.get(App.monitorList.getSelectedItem()).getDefaultConfiguration().getBounds();
                try
                {
                    Robot r = new Robot();
                    BufferedImage Image = r.createScreenCapture(capture);
                    App.afterImage = new TransparentImage(Image);
                }
                catch (AWTException e1)
                {
                    System.out.println(e1);
                }
            }
            
            App.f.add(App.afterImage);
            App.afterImage.setOpaque(false);
            App.afterImage.setVisible(false);
            App.f.setVisible(true);
            App.f.setLocation(
                App.monMap.get(App.monitorList.getSelectedItem())
                .getDefaultConfiguration().getBounds().x,
                App.monMap.get(App.monitorList.getSelectedItem())
                .getDefaultConfiguration().getBounds().y + App.f.getY());
            App.flashToggle = true;
            App.timer = new Timer();
            App.startFlashTwitch();   
        }         
    }
}