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

package bruma.utils;

/**
 * Boyer–Moore string search algorithm - 
 * http://en.wikipedia.org/wiki/Boyer%E2%80%93Moore_string_search_algorithm
 */
public class BoyerMoore {
  /**
   * Returns the index within this string of the first occurrence of the
   * specified substring. If it is not a substring, return -1.
   * 
   * @param haystack The string to be scanned
   * @param needle The target string to search
   * @return The start index of the substring
   */
  public static int indexOf(char[] haystack, char[] needle) {
    if (needle.length == 0) {
      return 0;
    }
    int charTable[] = makeCharTable(needle);
    int offsetTable[] = makeOffsetTable(needle);
    for (int i = needle.length - 1, j; i < haystack.length;) {
      for (j = needle.length - 1; needle[j] == haystack[i]; --i, --j) {
        if (j == 0) {
          return i;
        }
      }
      // i += needle.length - j; // For naive method
      i += Math.max(offsetTable[needle.length - 1 - j], charTable[haystack[i]]);
    }
    return -1;
  }
 
  /**
   * Makes the jump table based on the mismatched character information.
   */
  private static int[] makeCharTable(char[] needle) {
    final int ALPHABET_SIZE = 256;
    int[] table = new int[ALPHABET_SIZE];
    for (int i = 0; i < table.length; ++i) {
      table[i] = needle.length;
    }
    for (int i = 0; i < needle.length - 1; ++i) {
      table[needle[i]] = needle.length - 1 - i;
    }
    return table;
  }
 
  /**
   * Makes the jump table based on the scan offset which mismatch occurs.
   */
  private static int[] makeOffsetTable(char[] needle) {
    int[] table = new int[needle.length];
    int lastPrefixPosition = needle.length;
    for (int i = needle.length - 1; i >= 0; --i) {
      if (isPrefix(needle, i + 1)) {
        lastPrefixPosition = i + 1;
      }
      table[needle.length - 1 - i] = lastPrefixPosition - i + needle.length - 1;
    }
    for (int i = 0; i < needle.length - 1; ++i) {
      int slen = suffixLength(needle, i);
      table[slen] = needle.length - 1 - i + slen;
    }
    return table;
  }
 
  /**
   * Is needle[p:end] a prefix of needle?
   */
  private static boolean isPrefix(char[] needle, int p) {
    for (int i = p, j = 0; i < needle.length; ++i, ++j) {
      if (needle[i] != needle[j]) {
        return false;
      }
    }
    return true;
  }
 
  /**
   * Returns the maximum length of the substring ends at p and is a suffix.
   */
  private static int suffixLength(char[] needle, int p) {
    int len = 0;
    for (int i = p, j = needle.length - 1;
         i >= 0 && needle[i] == needle[j]; --i, --j) {
      len += 1;
    }
    return len;
  }
}
