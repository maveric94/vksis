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

    public static byte[] makePacket(byte[] rawData) {
        int packetsAmount = rawData.length / bytesPerPacket;

        if ((rawData.length % bytesPerPacket) > 0) {
            packetsAmount++;
        }

        packets.clear();

        for (int packetNumber = 0; packetNumber < packetsAmount; packetNumber++) {
            insertEndOfPacket();

            for (int byteNumber = 0; byteNumber < bytesPerPacket; byteNumber++) {
                int currentPosition = packetNumber * bytesPerPacket + byteNumber;
                byte currentByte;

                if (currentPosition >= rawData.length)
                    break;
                else
                    currentByte = rawData[currentPosition];

                for (int bitNumber = 7; bitNumber >= 0; bitNumber--) {
                    packets.add(((currentByte >> bitNumber) & 1) == 1);
                    checkForSequence(Operation.Add);
                }
            }

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


        return bitListToByteArray();
    }

    private static byte[] bitListToByteArray() {
        int packetsSize = packets.size();
        int bytesNumber = packetsSize / 8;
        byte[] processedData = new byte[bytesNumber];

        for (int i = 0; i < bytesNumber; i++) {
            byte b = 0;
            for (int j = 0; j < 8; j++) {
                b |= ((packets.get(i * 8 + j)) ? 1 : 0) << 7 - j;
            }
            processedData[i] = b;
        }

        return processedData;
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


}
