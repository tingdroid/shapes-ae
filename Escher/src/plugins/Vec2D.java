/*
 * Vec2D.java
 * 
 * This file is part of GeomLab
 * Copyright (c) 2005 J. M. Spivey
 * All rights reserved
 * 
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, 
 *    this list of conditions and the following disclaimer.      
 * 2. Redistributions in binary form must reproduce the above copyright notice, 
 *    this list of conditions and the following disclaimer in the documentation 
 *    and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products 
 *    derived from this software without specific prior written permission.
 *    
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR 
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES 
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; 
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, 
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF 
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package plugins;

import java.io.Serializable;

/** Floating point vectors in 2D */
public class Vec2D implements Serializable {
    private static final String svnid =
	"$Id: Vec2D.java 365 2008-06-11 17:11:29Z mike $";
    
    public final float x, y;
    
    public Vec2D(float x, float y) {
	this.x = x;
	this.y = y;
    }
    
    public float length() {
	return (float) Math.sqrt(x*x + y*y);
    }
    
    public Vec2D add(Vec2D a) {
	return new Vec2D(x + a.x, y + a.y);
    }
    
    public Vec2D add(float dx, float dy) {
	return new Vec2D(x + dx, y + dy);
    }

    public Vec2D scale(float r) {
	return new Vec2D(r*x, r*y);
    }
    
    public Vec2D rotate(float angle) {
	float c = cosd(angle), s = sind(angle);
	return new Vec2D(c*x-s*y, s*x+c*y);
    }
    
    public String toString() {
	return "Vec2D(" + x + ", " + y + ")";
    }

    static float cosd(float arg) {
        return (float) Math.cos(arg * Math.PI/180);
    }

    static float sind(float arg) {
        return (float) Math.sin(arg * Math.PI/180);
    }
}
