package dev.ramon.kees.shared.util;

import dev.ramon.kees.source.InvalidSourceFileException;
import lombok.experimental.UtilityClass;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

@UtilityClass
public final class DecodeUtf8 {

    public static String decode(byte[] bytes) {
        var decoder = StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);

        try {
            return decoder
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException exception) {
            throw new InvalidSourceFileException("File must contain valid UTF-8 text");
        }
    }
}
