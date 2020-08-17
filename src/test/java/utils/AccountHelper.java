package utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.util.StringUtil;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.UUID;
import java.util.stream.IntStream;

@Slf4j
public class AccountHelper {
    private AccountHelper() {
    }

    private static final int ADDRESS_LENGTH = 40;

    public static String generateRandomAddress() {
        SecureRandom secureRandom = new SecureRandom();
        return "0x" + IntStream.range(0, ADDRESS_LENGTH)
                .map(i -> (int) (secureRandom.nextDouble() * 2) == 0
                        ? (char) (secureRandom.nextDouble() * 10 + 48)
                        : (char) (secureRandom.nextDouble() * 6 + 'a')
                ).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    public static String generateRandomRootKey() {
        String key = UUID.randomUUID().toString();
        String rootKey = StringUtils.EMPTY;
        try {
            MessageDigest msdDigest = MessageDigest.getInstance("SHA-256");
            msdDigest.update(key.getBytes(StandardCharsets.UTF_8), 0, key.length());
            rootKey = DatatypeConverter.printHexBinary(msdDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            log.error("string covert error : {}", e.getMessage());
        }
        return rootKey;
    }
}
