package net.teamlixo.eggcrack;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LineLogFormatter extends Formatter {
    private String lineSeparator = "\n";
    public synchronized String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        String message = formatMessage(record);

        sb.append("[" + record.getLoggerName() + "] ");
        sb.append("[" + record.getLevel().getLocalizedName() + "] ");

        sb.append(message);
        sb.append(lineSeparator);

        if (record.getThrown() != null) {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                sb.append(sw.toString());
            } catch (Exception ex) {

            }
        }

        return sb.toString();
    }
}