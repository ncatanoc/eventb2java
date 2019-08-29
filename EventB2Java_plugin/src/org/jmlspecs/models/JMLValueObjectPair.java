// @(#)$Id: JMLPair.java-generic,v 1.31 2008/10/24 18:07:50 smshaner Exp $

// Copyright (C) 2005 Iowa State University
//
// This file is part of the runtime library of the Java Modeling Language.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public License
// as published by the Free Software Foundation; either version 2.1,
// of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with JML; see the file LesserGPL.txt.  If not, write to the Free
// Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
// 02110-1301  USA.


package org.jmlspecs.models;

/** Pairs of {@link K} and {@link V}, used in the types
 * {@link JMLValueToObjectRelation} and {@link JMLValueToObjectMap}.
 *
 * <p> In a pair the first element is called the "key" and the second
 * the "value". Both the key and the value in a pair must be non-null.
 *
 * @version $Revision: 1.31 $
 * @author Gary T. Leavens
 * @author Clyde Ruby
 * @see JMLValueToObjectRelation
 * @see JMLValueToObjectMap
 */
//-@ immutable
// FIXME: adapt this file to non-null-by-default and remove the following modifier.
/*@ nullable_by_default @*/ 
public /*@ pure @*/ class JMLValueObjectPair<K extends JMLType, V> implements JMLType {

    /** The key of this pair.
     */
    public final /*@ non_null @*/ K key;

    /** The value of this pair.
     */
    public final /*@ non_null @*/ V value;

    //@ public invariant owner == null;

    /*@ ensures dv != null && rv != null;
      @ signals_only NullPointerException;
      @ signals (NullPointerException) dv == null || rv == null;
      @*/
    /** Initialize the key and value of this pair.
     */
    public /*@ extract @*/ JMLValueObjectPair(/*@ non_null @*/ K dv,
                               /*@ non_null @*/ V rv)
        throws NullPointerException
    {
        if (dv == null) {
            throw new NullPointerException("Attempt to create a"
                                           + " JMLValueObjectPair with null"
                                           + " key");
        }
        if (rv == null) {
            throw new NullPointerException("Attempt to create a"
                                           + " JMLValueObjectPair with null"
                                           + " value");
        }
        //@ assume dv != null && rv != null;
        //@ set owner = null;
        key = (K)dv.clone();
        value = rv;
    }

    // inherit javadoc comment
    /*@ also
      @  public model_program {
      @    return new JMLValueObjectPair(key, value);
      @  }
      @*/
    public /*@ non_null @*/ Object clone()
    {
        return new JMLValueObjectPair<K, V>(key, value);
    } //@ nowarn Post;

    /** Tell whether this object's key is equal to the given key.
     * @see #equals
     */
    /*@  public normal_behavior
      @    ensures \result == (key.equals(dv));
      @*/
    public boolean keyEquals(K dv)
    {
        return key.equals(dv);
    }

    /** Tell whether this object's key is equal to the given key.
     * @see #equals
     */
    /*@  public normal_behavior
      @    ensures \result == (value == rv);
      @*/
    public boolean valueEquals(V rv)
    {
        return value == rv;
    }

    /** Test whether this object's value is equal to the given argument.
     * @see #keyEquals
     */
    /*@ also
      @  public normal_behavior
      @    requires obj != null && obj instanceof JMLValueObjectPair;
      @    ensures \result == 
      @            ( ((JMLValueObjectPair)obj).key.equals(key) 
      @              && ((JMLValueObjectPair)obj).value == value );
      @ also 
      @  public normal_behavior
      @    requires obj == null || !(obj instanceof JMLValueObjectPair);
      @    ensures !\result;
      @*/
    public boolean equals(/*@ nullable @*/ Object obj)
    {
        if (obj != null && obj instanceof JMLValueObjectPair) {
            JMLValueObjectPair pair = (JMLValueObjectPair)obj;
            return pair.key.equals(key) && pair.value == value;
        }
        else {
            return false;
        }
    }

    /** Return a hash code for this object.
     */
    public int hashCode() {
        return key.hashCode() + value.hashCode();
    }

    /** Return a string representation of this object.
     */
    /*@ also
      @  public normal_behavior
      @    ensures (* \result is a string that starts with a '(' followed by
      @                the strings representing key and value separated by
      @                a comma and ends with a ')' 
      @              *);
      @ for_example
      @   public normal_example
      @     requires (* key is 4 and value is 2 *);
      @     ensures (*  \result is the string "(4, 2)"  *);
      @*/
    public /*@ non_null @*/ String toString()
    {
        return(new String("(") + key + new String(", ") 
               + value + new String(")") );
    }
};
