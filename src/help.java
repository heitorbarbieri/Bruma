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

import bruma.master.MasterFactory;

/**
 *
 * @author Heitor Barbieri
 */
public class help {
    public static void main(final String[] args) {
        System.out.println("version:" + MasterFactory.VERSION + "\n");
        System.out.println("bruma.tools.lupa.Lupa - graphical database browser");
        System.out.println("bruma.examples.DumpDbIII - dumps the database records");
        System.out.println("bruma.tools.CopyMaster - copy a master file");
        System.out.println("bruma.tools.Isis2Mongo - export a database to MongoDb");
        System.out.println("bruma.tools.Isis2Couch - export a database to CouchDb");
        System.out.println("bruma.tools.ExportMaster - export a master file to other formats");
        System.out.println("bruma.tools.ImportMaster - import a master file from other formats");
        System.out.println("bruma.tools.RegExpImportMaster - import a master file using regular expressions");
        System.out.println("bruma.tools.Statistics - show statistics of a master file");
        System.out.println("bruma.tools.lupa.Search - search a regular expression in records");
        System.out.println("");
        System.out.println("");
        System.out.println("");
    }
}
