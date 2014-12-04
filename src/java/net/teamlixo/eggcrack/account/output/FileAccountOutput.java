package net.teamlixo.eggcrack.account.output;

import net.teamlixo.eggcrack.account.Account;
import net.teamlixo.eggcrack.credential.Credential;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileAccountOutput extends AccountOutput {
    private final File file;

    public FileAccountOutput(File file) {
        this.file = file;
    }

    @Override
    public void save(Account account, Credential credential) throws IOException {
        synchronized (file) {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file, true));

            bufferedWriter.write(account.getUsername() + ":" + credential.toString() + "\n");

            bufferedWriter.flush();
            bufferedWriter.close();
        }
    }
}
