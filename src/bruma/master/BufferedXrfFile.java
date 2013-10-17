/*=========================================================================

    Copyright © 2011 BIREME/PAHO/WHO

    This file is part of Bruma.

    Bruma is free software: you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as
    published by the Free Software Foundation, either version 3 of
    the License, or (at your option) any later version.

    Bruma is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with Bruma. If not, see <http://www.gnu.org/licenses/>.

=========================================================================*/

package bruma.master;

import bruma.BrumaException;
import java.io.IOException;
import java.util.Arrays;

/**
 *
 * @author Heitor Barbieri
 * @date 24/09/2008
 */
public class BufferedXrfFile extends XrfFile {
    private final int[] buffer;
    private int lastMfn;
    private boolean modified;
    private boolean autoCommit;

    /**
     * Create a new BufferedXrfFile object.
     * @param dbasename database name
     * @param shift master file shift
     * @param swapped tells if the master bytes are litle or big engian
     * @param create tells if the xrf file should be created.
     * @param lastMfn the last mfn of the master
     * @throws BrumaException
     */
    BufferedXrfFile(final String dbasename,
                    final int shift,
                    final boolean swapped,
                    final boolean create,
                    final int lastMfn) throws BrumaException {
        this(dbasename, shift, swapped, create, lastMfn, false);
    }

    /**
     * Create a new BufferedXrfFile object.
     * @param dbasename database name
     * @param shift master file shift
     * @param swapped tells if the master bytes are litle or big engian
     * @param create tells if the xrf file should be created.
     * @param lastMfn the last mfn of the master
     * @param autoCommit true if each xrf pointer change should be immediately
     *                   written to the file.
     * @throws BrumaException
     */
    BufferedXrfFile(final String dbasename,
                    final int shift,
                    final boolean swapped,
                    final boolean create,
                    final int lastMfn,
                    final boolean autoCommit) throws BrumaException {
        super(dbasename, shift, swapped, create);

        if (lastMfn < 0) {
            throw new BrumaException("BufferedXrfFile/lastMfn < 0");
        }
        this.lastMfn = lastMfn;
        this.autoCommit = autoCommit;
        buffer = new int[XRF_NRINBUFFER + 1];
        modified = false;
        /*try {
            fillBuffer(1);
        } catch(IOException ioe) {
            throw new BrumaException(ioe.getMessage());
        }*/
    }

    /**
     * Writes the buffer if necessary and closes files.
     * @throws BrumaException
     */
    @Override
    void close() throws BrumaException {
        try {
            writeBuffer();
        } catch(IOException ioe) {
            throw new BrumaException("close/" + ioe.getMessage());
        }
        super.close();
    }

    /**
     * Initializes the xrf file and the buffer.
     * @throws BrumaException
     */
    @Override
    void reset() throws BrumaException {
        super.reset();
        initializeBuffer(1);
    }

    /**
     * Reads the xrf info of a specif record.
     * @param mfn record id
     * @return XrfInfo - xrf pointer information
     * @throws BrumaException
     */
    @Override
    public XrfInfo readXrfInfo(final int mfn) throws BrumaException {
        final int packedMfp = buffer[getBufferPos(mfn)];

//System.out.println("mfn=" + mfn + " packedMfp=" + packedMfp);
        return readXrfInfoAux(mfn, packedMfp);
    }

    /**
     * Writes the xrf pointer of a record in the xrf file.
     * @param info XrfInfo xrf pointer info
     * @return the packed master file position
     * @throws BrumaException
     */
    @Override
    public int writeXrfInfo(final XrfInfo info) throws BrumaException {
        if (info == null) {
            throw new BrumaException("writeXrfInfo/null info");
        }
        final int block = Math.abs(info.getBlock());
        //final int bmax = (2 << (21 + shift - 1));   // valor maximo do bloco
        //final int bmax = (1 << (21 + shift)) - 1;   // valor maximo do bloco
        final int bmax = (1 << (20 + shift)) - 1;   // valor maximo do bloco
        final int lshift = (11 - shift);  // left shift
        int packedMfp = ((block << lshift) + (info.getOffset() >>> shift));

        if (info.getBlock() < 0) {
                packedMfp = -packedMfp;
        }
        if (block > bmax) {
            throw new BrumaException("writeXrfInfo/block[" + block + "] > "
             + bmax + ". Try increasing the master max size (shift) parameter."
             + " Current shift is " + shift + ".");
        }
        buffer[getBufferPos(info.getMfn())] = packedMfp;
        modified = true;
        try {
            if (autoCommit) {
                writeBuffer();
            }
        } catch (IOException ioe) {
            throw new BrumaException(ioe);
        }

        return packedMfp;
    }

    /**
     * Fills the buffer with zero and the block number in the first position
     * (negative if it is the last block).
     * @param bNum block number (positive value)
     */
    private void initializeBuffer(final int bNum) {
        assert bNum > 0;

        if (buffer != null) {
            Arrays.fill(buffer, 0);
            buffer[0] = -bNum;
        }
        modified = false;
//System.out.println("initializeBuffer - bNum=" + bNum);
    }

    /**
     * Returns the position in the buffer with the xrf pointer.
     * @param mfn specifies which xrf pointer is to be located.
     * @return buffer position
     * @throws BrumaException
     */
    private int getBufferPos(final int mfn) throws BrumaException {
        assert mfn > 0;

        if ((mfn <= 0) || (mfn > (lastMfn + 1))) {
            throw new BrumaException("getBufferPos/mfn[" + mfn +
                                                              "] out of range");
        }

        boolean newBlock = false;
        int cBlock = mfn / XRF_NRINBUFFER;
        int pos = mfn % XRF_NRINBUFFER;

        if (mfn == lastMfn + 1) {  // registro novo
            lastMfn++;
            newBlock = (pos == 1);
        }
        if (pos == 0) {
            pos = XRF_NRINBUFFER;
        } else {
            cBlock++;
        }
        if (cBlock != Math.abs(buffer[0])) {
//System.out.println("cBlock=" + cBlock + " Math.abs(buffer[0])="
//                                                        + Math.abs(buffer[0]));
            try {
                fillBuffer(cBlock, newBlock);
            } catch(IOException ioe) {
                throw new BrumaException(ioe);
            }
        }
//System.out.println("getBufferPos[mfn=" + mfn + "]=" + pos);
        assert pos > 0;
        return pos;
    }

    /**
     * Fills the buffer with xrf pointers or zeros if it is new.
     * @param bNum block number
     * @param newBlock read buffer (false) or fill it with zero (true)
     * @throws IOException
     */
    private void fillBuffer(final int bNum,
                            final boolean newBlock) throws IOException {
        assert bNum > 0;

//System.out.println("fillBuffer(bNum=" + bNum + ") - modified=" + modified
//                            + " newBlock=" + newBlock + " lastMfn=" + lastMfn);
        if (modified) {
            writeBuffer();
        }
        if (newBlock) {
            initializeBuffer(bNum);
        } else {
            readBuffer(bNum);
        }
    }

    /**
     * Fills the buffer with xrf pointers.
     * @param bNum xrf block number
     * @throws IOException
     */
    private void readBuffer(final int bNum) throws IOException {
        assert bNum > 0;

        fc.position((bNum - 1) * XRF_BLOCKSIZE);
        bBuffer.clear();
        fc.read(bBuffer);
        bBuffer.rewind();
        bBuffer.asIntBuffer().get(buffer);
        modified = false;
//System.out.println("readBuffer - posicao=" + ((bNum - 1) * XRF_BLOCKSIZE)
//                                                 + " buffer[0]=" + buffer[0]);
    }

    /**
     * If the pointer buffer is modified, it is written to the file.
     * @throws IOException
     */
    private void writeBuffer() throws IOException {
//System.out.println("writeBuffer - modified=" + modified + " bloco=" + buffer[0]);
        if (modified) {
            fc.position((Math.abs(buffer[0]) - 1) * XRF_BLOCKSIZE);
            bBuffer.clear();
            bBuffer.asIntBuffer().put(buffer);
            bBuffer.rewind();
            fc.write(bBuffer);
            modified = false;
        }
    }
}
