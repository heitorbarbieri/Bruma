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

package bruma.tools;

import bruma.master.Leader;
import bruma.master.SwapBytes;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 *
 * @author Heitor Barbieri
 */
public class DbType {
    private final RandomAccessFile raf;
    private final SwapBytes sb;

    private Leader readIsisWin() throws IOException {
        final Leader leader = new Leader();
        final int base;
        final int nvf;
        final int auxBase;

        raf.seek(64);
        leader.setMfn(sb.swap(raf.readInt()));
        leader.setMfrl(sb.swap(raf.readShort()));
        leader.setMfbwb(sb.swap(raf.readInt()));
        leader.setMfbwp(sb.swap(raf.readShort()));
        base = sb.swap(raf.readShort());
        leader.setBase(base);
        nvf = sb.swap(raf.readShort());
        leader.setNvf(nvf);

        auxBase = 18 + (nvf * 6);

        System.out.println("readIsisWin()=" + (base == auxBase));

        return leader;
    }

    private Leader readIsisLinux() throws IOException {
        final Leader leader = new Leader();
        final int base;
        final int nvf;
        final int auxBase;

        raf.seek(64);
        leader.setMfn(sb.swap(raf.readInt()));
        leader.setMfrl(sb.swap(raf.readShort()));
        raf.readShort();
        leader.setMfbwb(sb.swap(raf.readInt()));
        leader.setMfbwp(sb.swap(raf.readShort()));
        base = sb.swap(raf.readShort());
        leader.setBase(base);
        nvf = sb.swap(raf.readShort());
        leader.setNvf(nvf);

        auxBase = 18 + (nvf * 6);

        System.out.println("readIsisLinux()=" + (base == auxBase));

        return leader;
    }

    private Leader readFfiWin() throws IOException {
        final Leader leader = new Leader();
        final int base;
        final int nvf;
        final int auxBase;

        raf.seek(64);
        leader.setMfn(sb.swap(raf.readInt()));
        leader.setMfrl(sb.swap(raf.readInt()));
        leader.setMfbwb(sb.swap(raf.readInt()));
        leader.setMfbwp(sb.swap(raf.readShort()));
        base = sb.swap(raf.readInt());
        leader.setBase(base);
        nvf = sb.swap(raf.readShort());
        leader.setNvf(nvf);

        auxBase = 22 + 2 + (nvf * (10 + 2));

        System.out.println("readFfiWin()=" + (base == auxBase));

        return leader;
    }

    private Leader readFfiLinux() throws IOException {
        final Leader leader = new Leader();
        final int base;
        final int nvf;
        final int auxBase;

        raf.seek(64);
        leader.setMfn(sb.swap(raf.readInt()));
        leader.setMfrl(sb.swap(raf.readInt()));
        leader.setMfbwb(sb.swap(raf.readInt()));
        leader.setMfbwp(sb.swap(raf.readShort()));
        raf.readShort();
        base = sb.swap(raf.readInt());
        leader.setBase(base);
        nvf = sb.swap(raf.readShort());
        leader.setNvf(nvf);

        auxBase = 22 + 2 + (nvf * (10 + 2));

        System.out.println("readFfiLinux()=" + (base == auxBase));

        return leader;
    }

    public DbType(final String dbname,
                  final boolean swap) throws IOException {
        if (dbname == null) {
            throw new IllegalArgumentException();
        }
        raf = new RandomAccessFile(dbname, "r");
        sb = new SwapBytes(swap);
    }

    public void close() throws IOException {
        if (raf != null) {
            raf.close();
        }
    }

    public void printReport() throws IOException {
        System.out.println("Isis Windows version");
        System.out.println(readIsisWin());
        System.out.println();
        System.out.println("Isis Linux version");
        System.out.println(readIsisLinux());
        System.out.println();
        System.out.println("FFI Windows version");
        System.out.println(readFfiWin());
        System.out.println();
        System.out.println("FFI Linux version");
        System.out.println(readFfiLinux());
    }

    private static void usage() {
        System.err.println("usage: DbType <dbName> [--noswap]");
        System.exit(1);
    }

    public static void main(final String[] args) throws IOException {
        if (args.length < 1) {
            usage();
        }

        final boolean swapp = (args.length > 1) && (args[1].equals("--noswap"))
                                                                ? false : true;
        final DbType dbt = new DbType(args[0], swapp);

        dbt.printReport();
        dbt.close();
    }
}
