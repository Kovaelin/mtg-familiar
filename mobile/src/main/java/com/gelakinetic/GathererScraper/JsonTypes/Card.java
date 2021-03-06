/*
 * Copyright 2017 Adam Feinstein
 *
 * This file is part of MTG Familiar.
 *
 * MTG Familiar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MTG Familiar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MTG Familiar.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.gelakinetic.GathererScraper.JsonTypes;

import android.support.annotation.NonNull;

import com.gelakinetic.mtgfam.helpers.database.CardDbAdapter;

import java.util.ArrayList;

/*
 * This class contains all information about a scraped card
 *
 * @author AEFeinstein
 *
 */
public class Card implements Comparable<Card> {

    // The card's name
    public String mName = "";

    // The card's mana cost
    public String mManaCost = "";

    // The card's converted mana cost
    public int mCmc = 0;

    // The card's type, includes super and sub
    public String mType = "";

    // The card's text text
    public String mText = "";

    // The card's flavor text
    public String mFlavor = "";

    // The card's expansion
    public String mExpansion = "";

    // The card's rarity
    public char mRarity = '\0';

    // The card's collector's number. Not an integer (i.e. 181a, 181b)
    public String mNumber = "";

    // The card's artist
    public String mArtist = "";

    // The card's colors
    public String mColor = "";

    // The card's colors
    public String mColorIdentity = "";

    // The card's multiverse id
    public int mMultiverseId = 0;

    // The card's power. Not an integer (i.e. *+1, X)
    public float mPower = CardDbAdapter.NO_ONE_CARES;

    // The card's toughness, see mPower
    public float mToughness = CardDbAdapter.NO_ONE_CARES;

    // The card's loyalty. An integer in practice
    public int mLoyalty = CardDbAdapter.NO_ONE_CARES;

    // All the card's foreign printings
    public ArrayList<ForeignPrinting> mForeignPrintings = new ArrayList<>();

    // The card's loyalty. An integer in practice
    public String mWatermark = "";

    // Private class for encapsulating foreign printing information
    public static class ForeignPrinting implements Comparable<ForeignPrinting> {
        public int mMultiverseId;
        public String mName;
        public String mLanguageCode;

        @Override
        public int compareTo(@NonNull ForeignPrinting o) {
            return Integer.valueOf(this.mMultiverseId).compareTo(o.mMultiverseId);
        }

        @Override
        public boolean equals(Object arg0) {
            return (arg0 instanceof ForeignPrinting) &&
                    (this.mMultiverseId == ((ForeignPrinting) arg0).mMultiverseId);
        }
    }

    /**
     * This function usually sorts by collector's number. However, gatherer
     * doesn't have collector's number for expansions before collector's number
     * was printed, and magiccards.info uses a strange numbering scheme. This
     * function does it's best
     */
    @Override
    public int compareTo(@NonNull Card other) {

        /* Sort by collector's number */
        if (this.mNumber != null && other.mNumber != null && this.mNumber.length() > 0 && other.mNumber.length() > 0) {

            int this_num = this.getNumberInteger();
            int other_num = other.getNumberInteger();
            if (this_num > other_num) {
                return 1;
            } else if (this_num < other_num) {
                return -1;
            } else {
                return Character.valueOf(this.getNumberChar()).compareTo(other.getNumberChar());
            }
        }

        /* Battle Royale is pure alphabetical, except for basics, why not */
        if (this.mExpansion.equals("BR")) {
            if (this.mType.contains("Basic Land") && !other.mType.contains("Basic Land")) {
                return 1;
            }
            if (!this.mType.contains("Basic Land") && other.mType.contains("Basic Land")) {
                return -1;
            }
            return this.mName.compareTo(other.mName);
        }

        /*
         * Or if that doesn't exist, sort by color order. Weird for
         * magiccards.info
         */
        if (this.getNumFromColor() > other.getNumFromColor()) {
            return 1;
        } else if (this.getNumFromColor() < other.getNumFromColor()) {
            return -1;
        }

        /* If the color matches, sort by name */
        return this.mName.compareTo(other.mName);
    }

    /**
     * Returns a number used for sorting by color. This is different for
     * Beatdown because magiccards.info is weird
     *
     * @return A number indicating how the card's color is sorted
     */
    private int getNumFromColor() {
        /* Because Beatdown properly sorts color */
        if (this.mExpansion.equals("BD")) {
            if (this.mColor.length() > 1) {
                return 7;
            }
            switch (this.mColor.charAt(0)) {
                case 'W': {
                    return 0;
                }
                case 'U': {
                    return 1;
                }
                case 'B': {
                    return 2;
                }
                case 'R': {
                    return 3;
                }
                case 'G': {
                    return 4;
                }
                case 'A': {
                    return 5;
                }
                case 'L': {
                    return 6;
                }
            }
        }
        /* And magiccards.info has weird numbering for everything else */
        else {
            if (this.mColor.length() > 1) {
                return 7;
            }
            switch (this.mColor.charAt(0)) {
                case 'B': {
                    return 0;
                }
                case 'U': {
                    return 1;
                }
                case 'G': {
                    return 2;
                }
                case 'R': {
                    return 3;
                }
                case 'W': {
                    return 4;
                }
                case 'A': {
                    return 5;
                }
                case 'L': {
                    return 6;
                }
            }
        }
        return 8;
    }

    private int getNumberInteger() {
        try {
            char c = this.mNumber.charAt(this.mNumber.length() - 1);
            if (('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z')) {
                return Integer.parseInt(this.mNumber.substring(0, this.mNumber.length() - 1));
            }
            return Integer.parseInt(this.mNumber);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private char getNumberChar() {
        char c = this.mNumber.charAt(this.mNumber.length() - 1);
        if (('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z')) {
            return c;
        }
        return 0;
    }
}