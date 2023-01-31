/*
 * Copyright 2018 Viritin.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.vaadin.firitin;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinSession;
import org.vaadin.firitin.components.DynamicFileDownloader;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author mstahv
 */
@Route
public class DynamicFileDownloadingDemo extends VerticalLayout {

    private boolean cancelled = false;

    private DynamicFileDownloader actaulButtonLikeDownloadButton = null;

    private DynamicFileDownloader downloadThatNotifiesWhenReady;

    public DynamicFileDownloadingDemo() {

        DynamicFileDownloader downloadButton = new DynamicFileDownloader("Download foobar.txt", "foobar.txt",
        outputStream -> {
            try {
                /**
                 * Note that the filename in this example is static.
                 * Also setting filename here wouldn't affect anymore
                 * as the http headers have already been sent.
                 * downloadButton.setFileName("too-late.txt");
                 *
                 * Check the next example to see how t define the name
                 * when the actual download is happening.
                 */
                outputStream.write("HelloWorld".getBytes());
            } catch (IOException ex) {
                Logger.getLogger(DynamicFileDownloadingDemo.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        
        downloadButton.setTarget("_new");
        
        add(downloadButton);
        
        DynamicFileDownloader downloadButton2 = new DynamicFileDownloader("Downlload file with timestamp in name", "foobar/",
        outputStream -> {
            try {
                outputStream.write("HelloWorld".getBytes());
            } catch (IOException ex) {
                Logger.getLogger(DynamicFileDownloadingDemo.class.getName()).log(Level.SEVERE, null, ex);
            }
        }) {
            /**
             * To define the name of file dynamically for each download,
             * we need to override this method. It gets called just before
             * the actual content will be written.
             *
             * @param session the vaadin session
             * @param request the vaadin request
             * @return the name of the file on the end users computer. In this
             * example we just prefix "foobar.txt" with timestamp.
             *
             */
            @Override
            protected String getFileName(VaadinSession session, VaadinRequest request) {
                return LocalDateTime.now() + "foobar.txt";
            }
        };
        
        add(downloadButton2);


        UI ui = UI.getCurrent();
        ui.setPollInterval(500); // simulate Push, not needed if using Push
        downloadThatNotifiesWhenReady = new DynamicFileDownloader("Download that notifies the UI when finished", "foobar/",
                outputStream -> {
                    try {
                        outputStream.write("HelloWorld".getBytes());
                    } catch (IOException ex) {
                        Logger.getLogger(DynamicFileDownloadingDemo.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
        );
        downloadThatNotifiesWhenReady.addDownloadFinishedListener(e->{
            Notification.show("Download is now finished");
            // you could do something else here as well, like removing the downloader
            remove(downloadThatNotifiesWhenReady);
        });

        add(downloadThatNotifiesWhenReady);

        // Note, the styling of disabled download (read anchor) is currently broken,
        // ought to be fixed in Vaadin 23
        DynamicFileDownloader disableOnClick = new DynamicFileDownloader("Allow just one download per 10 secs", "foobar.txt",
                outputStream -> {
                    try {
                        outputStream.write("HelloWorld".getBytes());
                    } catch (IOException ex) {
                        Logger.getLogger(DynamicFileDownloadingDemo.class.getName()).log(Level.SEVERE, null, ex);
                    }
                });
        disableOnClick.setDisableOnClick(true);
        disableOnClick.addDownloadFinishedListener(e-> {
            new Thread() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    disableOnClick.getUI().ifPresent(ui -> {
                           ui.access(() -> disableOnClick.setEnabled(true));
                    });
                }
            }.start();
        });
        add(disableOnClick);


        UI.getCurrent().setPollInterval(500);

        actaulButtonLikeDownloadButton = new DynamicFileDownloader("Download foobar.txt", "foobar.txt",
                outputStream -> {
                    try {
                        outputStream.write("HelloWorld".getBytes());
                        try {
                            Thread.sleep(4000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        outputStream.write("HelloWorld".getBytes());
                        if(true)
                            throw new RuntimeException("Die");
                        outputStream.write("HelloWorld".getBytes());

                    } catch (IOException ex) {
                        Logger.getLogger(DynamicFileDownloadingDemo.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }).asButton();
        actaulButtonLikeDownloadButton.setDisableOnClick(true);
        actaulButtonLikeDownloadButton.addDownloadFinishedListener(e -> {
            actaulButtonLikeDownloadButton.setEnabled(true);
        });
        actaulButtonLikeDownloadButton.getButton().setIcon(VaadinIcon.DOWNLOAD.create());

        add(actaulButtonLikeDownloadButton);

        DynamicFileDownloader interruptable = new DynamicFileDownloader("Download foobar.txt (interrupt-able)", "foobar.txt",
                outputStream -> {
                    try {

                        for (int i = 0; i < 10; i++) {
                            if (cancelled) {
                                throw new RuntimeException("Die");
                            }
                            outputStream.write("HelloWorld".getBytes());
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }

                    } catch (IOException ex) {
                        Logger.getLogger(DynamicFileDownloadingDemo.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }).asButton();

        Button b = new Button("Cancel file generation");
        b.addClickListener( e -> {
            cancelled = true;
        });
        add(interruptable, b);

        DynamicFileDownloader withError = new DynamicFileDownloader("Download that fails", "foobar.txt",
                outputStream -> {
                    throw new RuntimeException("Fail on purpose!");
                }).asButton();
        
        withError.addDownloadFailedListener(e -> {
            Notification.show(e.getException().getMessage() +" Note, file may have been generated on some browsers. Chrome should not do it.");
        });
        add(withError);
    }


}
