package com.example;

import java.util.Timer;
import java.util.function.Consumer;
import java.awt.*;
import java.awt.image.BufferedImage;

import com.github.twitch4j.chat.events.channel.SubscriptionEvent;
import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import static com.sun.jna.platform.win32.WinUser.GWL_EXSTYLE;
import static com.sun.jna.platform.win32.WinUser.WS_EX_LAYERED;
import static com.sun.jna.platform.win32.WinUser.WS_EX_TRANSPARENT;

/*
 * Consumer Class for Subscriptions
 * Performs the same functions as the startFlash() function in the main App class, but slightly cut down
 */

class FlashbangSubConsumer<E> implements Consumer<SubscriptionEvent>
{
    @Override
    public void accept(SubscriptionEvent t) {
        App.f.add(App.firstComponent);
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
            
        App.f.setLocation(
            App.monMap.get(App.monitorList.getSelectedItem())
            .getDefaultConfiguration().getBounds().x,
            App.monMap.get(App.monitorList.getSelectedItem())
            .getDefaultConfiguration().getBounds().y + App.f.getY());

        App.f.add(App.afterImage);
        App.afterImage.setOpaque(false);
        App.afterImage.setVisible(false);
    
        App.firstComponent.setVisible(true);
        App.f.setVisible(true);
                    
        final HWND hwnd = new HWND(Native.getComponentPointer(App.f));
        final User32 user32 = User32.INSTANCE;
        int exStyle = user32.GetWindowLong(hwnd, GWL_EXSTYLE);
        user32.SetWindowLong(hwnd, GWL_EXSTYLE, exStyle | WS_EX_LAYERED | WS_EX_TRANSPARENT | 0x00000080);
        user32.SetLayeredWindowAttributes(hwnd, 0, (byte)255, 0x2);
        user32.UpdateWindow(hwnd);

        App.flashToggle = true;
        App.timer = new Timer();
        App.startFlashTwitch();          
    }
}