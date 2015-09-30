package maveric.vksis;

import java.util.ArrayList;
import java.util.Arrays;


/**
 * Created by Maveric on 13/09/2015.
 */
public class Packet {

    private enum Operation {
      Extract,
        Add,

    };
    public static int bytesPerPacket = 10;
    private static ArrayList<Boolean> forbiddenSequence = new ArrayList<>(Arrays.asList(false, true, true, true, true, true, true));
    private static ArrayList<Boolean> stuffedSequence = new ArrayList<>(Arrays.asList(false, true, true, true, true, true, true, true));
    private static ArrayList<Boolean> endOfPacket = new ArrayList<>(Arrays.asList(false, true, true, true, true, true, true, false));
    private static ArrayList<Boolean> packets = new ArrayList<>();

    public static byte[] makePacket(byte[] rawData, boolean addError) {
        int packetsAmount = rawData.length / bytesPerPacket;

        if ((rawData.length % bytesPerPacket) > 0) {
            packetsAmount++;
        }

        packets.clear();

        for (int packetNumber = 0; packetNumber < packetsAmount; packetNumber++) {
            insertEndOfPacket();

            int currentPosition = packetNumber * bytesPerPacket;
            insertCheckSum(rawData, currentPosition);

            for (int byteNumber = 0; byteNumber < bytesPerPacket; byteNumber++) {
                //int currentPosition = packetNumber * bytesPerPacket + byteNumber;
                byte currentByte;

                if (currentPosition + byteNumber >= rawData.length)
                    break;
                else
                    currentByte = rawData[currentPosition + byteNumber];

                addByteToPacket(currentByte);
            }

            if (addError)
                if (packets.get(packets.size() - 1))
                    packets.set(packets.size() - 1, false);
                else
                    packets.set(packets.size() - 1, true);

            insertEndOfPacket();
        }

        return bitListToByteArray();
    }
    public static byte[] extractFromPacket(byte[] packet) {
        int bitsDeleted = 0;
        boolean bitDeleted = false;
        packets.clear();

        for (int byteNumber = 1; byteNumber < packet.length - 1; byteNumber++) {
            for (int bitNumber = 7; bitNumber >= 0; bitNumber--) {
                packets.add(((packet[byteNumber] >> bitNumber) & 1) == 1);
                if (!bitDeleted && checkForSequence(Operation.Extract)) {
                    bitDeleted = true;
                    bitsDeleted++;
                    continue;
                }
                bitDeleted = false;
            }
        }

        int bitsToDelete = 8 - (bitsDeleted % 8);
        if (bitsToDelete != 8) {
            for (int i = 0; i < bitsToDelete; i++) {
                packets.remove(packets.size() - 1);
            }
        }

        //extract checksumm
        byte expectedChecksumm = 0;
        for (int i = 0; i < 8; i++) {
            expectedChecksumm |= ((packets.get(0)) ? 1 : 0) << 7 - i;
            packets.remove(0);
        }

        byte[] extractedMessage = bitListToByteArray();

        CRC8.clearValue();
        CRC8.updateChecksumm(extractedMessage);
        byte checksumm = CRC8.getValue();

        if (checksumm != expectedChecksumm)
            return null;

        return extractedMessage;
    }

    private static byte[] bitListToByteArray() {
        int packetsSize = packets.size();
        int bytesNumber = packetsSize / 8;
        byte[] processedData = new byte[bytesNumber];

        for (int i = 0; i < bytesNumber; i++)
            processedData[i] = extractByte(i);

        return processedData;
    }

    private static byte extractByte(int number) {
        byte b = 0;

        for (int i = 0; i < 8; i++)
            b |= ((packets.get(number * 8 + i)) ? 1 : 0) << 7 - i;

        return b;
    }


    private static boolean checkForSequence(Operation op) {
        ArrayList<Boolean> sequence;
        if (op == Operation.Add)
            sequence = forbiddenSequence;
        else
            sequence = stuffedSequence;

        int sequenceSize = sequence.size();
        int packetSize = packets.size();

        if (packetSize < sequenceSize)
            return false;

        for (int i = 1; i <= sequenceSize; i++) {
            if (packets.get(packetSize - i) != sequence.get(sequenceSize - i)) {
                return false;
            }
        }

        if (op == Operation.Add)
            packets.add(true);
        else
            packets.remove(packetSize - 1);

        return true;
    }
    private static void insertEndOfPacket() {
        int eopSize = endOfPacket.size();
        int packetsSize = packets.size();
        int lastBits = packetsSize % 8;
        //filling the last byte cause we need endofpacket symbol to be aligned to byte border
        if (lastBits > 0) {
            for (int i = 0; i < 8 - lastBits; i++) {
                packets.add(false);
            }
        }

        for (int i = 0; i < eopSize; i++) {
            packets.add(endOfPacket.get(i));
        }
    }

    private static void insertCheckSum(byte[] data, int currentPos) {
        CRC8.clearValue();

        for (int i = 0; i < bytesPerPacket; i++) {
            if (currentPos + i >= data.length)
                break;
            CRC8.updateChecksumm(data[currentPos + i]);
        }


        addByteToPacket(CRC8.getValue());

    }

    private static void addByteToPacket(byte b) {
        for (int bitNumber = 7; bitNumber >= 0; bitNumber--) {
            packets.add(((b >> bitNumber) & 1) == 1);
            checkForSequence(Operation.Add);
        }
    }



}
