package net.teamlixo.eggcrack.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class HeaderImage extends JComponent {
    private Image image;
    public HeaderImage() throws IOException {
        this.setSize(new Dimension(500, 100));
        image = ImageIO.read(this.getClass().getResourceAsStream("/net/teamlixo/eggcrack/eggcrack_2.0.png"));
    }

    @Override
    public void paint(Graphics g) {
        g.drawImage(image, 0, -25, null);
    }
}
