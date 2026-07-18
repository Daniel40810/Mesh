package com.dan.ficons;

import com.dan.ficons.FIcon;
import com.dan.ficons.FIconPaint;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

public enum FIconType implements FIcon
{
    ADD("Hinzuf\u00fcgen"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim;
            float pad = d * 0.12f;
            float sz = d - 2.0f * pad;
            float cx = d / 2.0f;
            float cy = d / 2.0f;
            Ellipse2D.Float circ = new Ellipse2D.Float(pad, pad, sz, sz);
            g2.setPaint(FIconPaint.liquid(pad, pad, sz, sz));
            g2.fill(circ);
            FIconType.clipShine(g2, circ, pad, pad, sz, sz);
            FIconPaint.rim(g2, circ, Math.max(1.0f, d * 0.045f));
            g2.setStroke(FIconType.round(d * 0.11f));
            g2.setColor(FIconPaint.INK);
            float arm = sz * 0.27f;
            g2.draw(new Line2D.Float(cx - arm, cy, cx + arm, cy));
            g2.draw(new Line2D.Float(cx, cy - arm, cx, cy + arm));
        }
    }
    ,
    DELETE("L\u00f6schen"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim;
            float pad = d * 0.18f;
            float w = d - 2.0f * pad;
            float lidH = Math.max(2.0f, d * 0.1f);
            float top = pad + d * 0.06f;
            float bodyY = top + lidH + d * 0.03f;
            float bodyH = d - pad - bodyY;
            g2.setColor(FIconPaint.WINE_RED);
            g2.fill(new RoundRectangle2D.Float(d / 2.0f - w * 0.16f, pad, w * 0.32f, d * 0.07f, 3.0f, 3.0f));
            RoundRectangle2D.Float lid = new RoundRectangle2D.Float(pad - d * 0.02f, top, w + d * 0.04f, lidH, lidH, lidH);
            g2.fill(lid);
            RoundRectangle2D.Float body = new RoundRectangle2D.Float(pad + d * 0.03f, bodyY, w - d * 0.06f, bodyH, d * 0.12f, d * 0.12f);
            g2.setPaint(FIconPaint.liquid(pad, bodyY, w, bodyH));
            g2.fill(body);
            FIconType.clipShine(g2, body, pad, bodyY, w, bodyH);
            FIconPaint.rim(g2, body, Math.max(1.0f, d * 0.04f));
            g2.setColor(FIconPaint.alpha(FIconPaint.INK, 150));
            g2.setStroke(FIconType.round(Math.max(1.0f, d * 0.035f)));
            for (int i = 1; i <= 2; ++i) {
                float x = pad + d * 0.03f + (w - d * 0.06f) * ((float)i / 3.0f);
                g2.draw(new Line2D.Float(x, bodyY + bodyH * 0.18f, x, bodyY + bodyH * 0.82f));
            }
        }
    }
    ,
    SAVE("Speichern"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim;
            float pad = d * 0.15f;
            float sz = d - 2.0f * pad;
            float cut = sz * 0.24f;
            Path2D.Float body = new Path2D.Float();
            ((Path2D)body).moveTo(pad, pad);
            ((Path2D)body).lineTo(pad + sz - cut, pad);
            ((Path2D)body).lineTo(pad + sz, pad + cut);
            ((Path2D)body).lineTo(pad + sz, pad + sz);
            ((Path2D)body).lineTo(pad, pad + sz);
            body.closePath();
            g2.setPaint(FIconPaint.liquid(pad, pad, sz, sz));
            g2.fill(body);
            FIconType.clipShine(g2, body, pad, pad, sz, sz);
            FIconPaint.rim(g2, body, Math.max(1.0f, d * 0.04f));
            g2.setColor(FIconPaint.DARK_HOLE);
            g2.fill(new Rectangle2D.Float(pad + sz * 0.22f, pad, sz * 0.46f, sz * 0.3f));
            g2.setColor(FIconPaint.alpha(FIconPaint.INK, 210));
            g2.fill(new Rectangle2D.Float(pad + sz * 0.55f, pad + sz * 0.04f, sz * 0.08f, sz * 0.2f));
            g2.setColor(FIconPaint.alpha(FIconPaint.INK, 230));
            g2.fill(new RoundRectangle2D.Float(pad + sz * 0.18f, pad + sz * 0.46f, sz * 0.64f, sz * 0.4f, sz * 0.08f, sz * 0.08f));
            g2.setColor(FIconPaint.alpha(FIconPaint.WINE_RED, 200));
            g2.setStroke(FIconType.round(Math.max(1.0f, d * 0.03f)));
            g2.draw(new Line2D.Float(pad + sz * 0.28f, pad + sz * 0.6f, pad + sz * 0.72f, pad + sz * 0.6f));
            g2.draw(new Line2D.Float(pad + sz * 0.28f, pad + sz * 0.72f, pad + sz * 0.62f, pad + sz * 0.72f));
        }
    }
    ,
    EDIT("Bearbeiten"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim;
            AffineTransform old = g2.getTransform();
            g2.rotate(Math.toRadians(45.0), d / 2.0f, d / 2.0f);
            float pw = d * 0.22f;
            float px = d / 2.0f - pw / 2.0f;
            float top = d * 0.15f;
            float bot = d * 0.85f;
            g2.setColor(FIconPaint.WINE_RED);
            g2.fill(new RoundRectangle2D.Float(px, top, pw, d * 0.12f, pw * 0.4f, pw * 0.4f));
            float shaftTop = top + d * 0.11f;
            float shaftBot = bot - d * 0.18f;
            Rectangle2D.Float shaft = new Rectangle2D.Float(px, shaftTop, pw, shaftBot - shaftTop);
            g2.setPaint(FIconPaint.liquid(px, shaftTop, pw, shaftBot - shaftTop));
            g2.fill(shaft);
            FIconType.clipShine(g2, shaft, px, shaftTop, pw, shaftBot - shaftTop);
            Path2D.Float tip = new Path2D.Float();
            ((Path2D)tip).moveTo(px, shaftBot);
            ((Path2D)tip).lineTo(px + pw, shaftBot);
            ((Path2D)tip).lineTo(d / 2.0f, bot);
            tip.closePath();
            g2.setColor(FIconPaint.alpha(FIconPaint.INK, 235));
            g2.fill(tip);
            Path2D.Float lead = new Path2D.Float();
            ((Path2D)lead).moveTo(d / 2.0f - pw * 0.22f, bot - d * 0.07f);
            ((Path2D)lead).lineTo(d / 2.0f + pw * 0.22f, bot - d * 0.07f);
            ((Path2D)lead).lineTo(d / 2.0f, bot);
            lead.closePath();
            g2.setColor(new Color(32, 32, 32));
            g2.fill(lead);
            g2.setTransform(old);
        }
    }
    ,
    SEARCH("Suchen"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim;
            float r = d * 0.25f;
            float cx = d * 0.42f;
            float cy = d * 0.42f;
            Ellipse2D.Float inner = new Ellipse2D.Float(cx - r, cy - r, 2.0f * r, 2.0f * r);
            g2.setColor(FIconPaint.GLASS_BODY);
            g2.fill(inner);
            FIconType.clipShine(g2, inner, cx - r, cy - r, 2.0f * r, 2.0f * r);
            g2.setColor(FIconPaint.WINE_RED);
            g2.setStroke(new BasicStroke(d * 0.13f, 1, 1));
            float hx = cx + (float)Math.cos(0.7853981633974483) * r;
            float hy = cy + (float)Math.sin(0.7853981633974483) * r;
            g2.draw(new Line2D.Float(hx, hy, d * 0.84f, d * 0.84f));
            g2.setStroke(new BasicStroke(d * 0.11f, 1, 1));
            g2.setPaint(FIconPaint.liquidDiagonal(cx - r, cy - r, cx + r, cy + r));
            g2.draw(inner);
        }
    }
    ,
    REFRESH("Aktualisieren"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim;
            float pad = d * 0.16f;
            float sz = d - 2.0f * pad;
            float cx = d / 2.0f;
            float cy = d / 2.0f;
            float r = sz / 2.0f;
            float start = 65.0f;
            float extent = 255.0f;
            Arc2D.Float arc = new Arc2D.Float(pad, pad, sz, sz, start, extent, 0);
            g2.setStroke(new BasicStroke(d * 0.12f, 0, 1));
            g2.setPaint(FIconPaint.liquidDiagonal(pad, pad, pad + sz, pad + sz));
            g2.draw(arc);
            double end = Math.toRadians(start + extent);
            float ex = cx + (float)Math.cos(end) * r;
            float ey = cy - (float)Math.sin(end) * r;
            double tan = end + 1.5707963267948966;
            float tx = (float)Math.cos(tan);
            float ty = -((float)Math.sin(tan));
            float nx = (float)Math.cos(end);
            float ny = -((float)Math.sin(end));
            float a = d * 0.16f;
            Path2D.Float head = new Path2D.Float();
            ((Path2D)head).moveTo(ex + tx * a, ey + ty * a);
            ((Path2D)head).lineTo(ex - nx * a * 0.9f, ey - ny * a * 0.9f);
            ((Path2D)head).lineTo(ex + nx * a * 0.9f, ey + ny * a * 0.9f);
            head.closePath();
            g2.setColor(FIconPaint.TURQUOISE);
            g2.fill(head);
        }
    }
    ,
    SETTINGS("Einstellungen"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim;
            float cx = d / 2.0f;
            float cy = d / 2.0f;
            float rOut = d * 0.42f;
            float toothW = d * 0.16f;
            float toothH = d * 0.16f;
            g2.setPaint(FIconPaint.liquid(d * 0.1f, d * 0.1f, d * 0.8f, d * 0.8f));
            RoundRectangle2D.Float tooth = new RoundRectangle2D.Float(cx - toothW / 2.0f, cy - rOut, toothW, toothH, toothW * 0.4f, toothW * 0.4f);
            for (int k = 0; k < 8; ++k) {
                AffineTransform at = AffineTransform.getRotateInstance(Math.toRadians((double)k * 45.0), cx, cy);
                g2.fill(at.createTransformedShape(tooth));
            }
            float br = d * 0.3f;
            Ellipse2D.Float body = new Ellipse2D.Float(cx - br, cy - br, 2.0f * br, 2.0f * br);
            g2.fill(body);
            FIconType.clipShine(g2, body, cx - br, cy - br, 2.0f * br, 2.0f * br);
            FIconPaint.rim(g2, body, Math.max(1.0f, d * 0.04f));
            float hr = d * 0.13f;
            g2.setColor(FIconPaint.DARK_HOLE);
            g2.fill(new Ellipse2D.Float(cx - hr, cy - hr, 2.0f * hr, 2.0f * hr));
            g2.setColor(FIconPaint.TURQUOISE);
            float dr = d * 0.05f;
            g2.fill(new Ellipse2D.Float(cx - dr, cy - dr, 2.0f * dr, 2.0f * dr));
        }
    }
    ,
    DATABASE("Datenbank"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim;
            float pad = d * 0.18f;
            float w = d - 2.0f * pad;
            float ellH = Math.max(3.0f, d * 0.18f);
            float top = pad;
            float bot = d - pad;
            float midTop = top + ellH / 2.0f;
            float midBot = bot - ellH / 2.0f;
            Rectangle2D.Float rect = new Rectangle2D.Float(pad, midTop, w, midBot - midTop);
            g2.setPaint(FIconPaint.liquid(pad, top, w, bot - top));
            g2.fill(rect);
            g2.fill(new Ellipse2D.Float(pad, bot - ellH, w, ellH));
            Area bodyArea = new Area(rect);
            bodyArea.add(new Area(new Ellipse2D.Float(pad, bot - ellH, w, ellH)));
            bodyArea.add(new Area(new Ellipse2D.Float(pad, top, w, ellH)));
            FIconType.clipShine(g2, bodyArea, pad, top, w, bot - top);
            g2.setColor(FIconPaint.alpha(FIconPaint.INK, 150));
            g2.setStroke(FIconType.round(Math.max(1.0f, d * 0.035f)));
            float h = midBot - midTop;
            for (float f : new float[]{0.34f, 0.67f}) {
                float yy = midTop + h * f - ellH / 2.0f;
                g2.draw(new Arc2D.Float(pad, yy, w, ellH, 180.0f, 180.0f, 0));
            }
            Ellipse2D.Float lid = new Ellipse2D.Float(pad, top, w, ellH);
            g2.setColor(new Color(46, 217, 208));
            g2.fill(lid);
            FIconPaint.rim(g2, lid, Math.max(1.0f, d * 0.04f));
        }
    }
    ,
    GEO("Geo / Karte"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim;
            float pad = d * 0.13f;
            float sz = d - 2.0f * pad;
            float cx = d / 2.0f;
            float cy = d / 2.0f;
            float r = sz / 2.0f;
            Ellipse2D.Float globe = new Ellipse2D.Float(pad, pad, sz, sz);
            g2.setPaint(FIconPaint.liquid(pad, pad, sz, sz));
            g2.fill(globe);
            FIconType.clipShine(g2, globe, pad, pad, sz, sz);
            Shape oc = g2.getClip();
            g2.clip(globe);
            g2.setColor(FIconPaint.alpha(FIconPaint.INK, 175));
            g2.setStroke(FIconType.round(Math.max(1.0f, d * 0.028f)));
            g2.draw(new Line2D.Float(pad, cy, pad + sz, cy));
            g2.draw(new Line2D.Float(pad, cy - r * 0.52f, pad + sz, cy - r * 0.52f));
            g2.draw(new Line2D.Float(pad, cy + r * 0.52f, pad + sz, cy + r * 0.52f));
            g2.draw(new Line2D.Float(cx, pad, cx, pad + sz));
            g2.draw(new Ellipse2D.Float(cx - sz * 0.27f, pad, sz * 0.54f, sz));
            g2.setClip(oc);
            FIconPaint.rim(g2, globe, Math.max(1.0f, d * 0.045f));
        }
    }
    ,
    LOCATION("Standort"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim;
            float cx = d / 2.0f;
            float w = d * 0.54f;
            float x = cx - w / 2.0f;
            float topY = d * 0.12f;
            float tipY = d * 0.9f;
            float r = w / 2.0f;
            float cyc = topY + r;
            Path2D.Float pin = new Path2D.Float();
            ((Path2D)pin).moveTo(cx, tipY);
            ((Path2D)pin).quadTo(x, cyc + r * 0.55f, x, cyc);
            Arc2D.Float top = new Arc2D.Float(x, topY, w, w, 180.0f, -180.0f, 0);
            pin.append(top, true);
            ((Path2D)pin).quadTo(x + w, cyc + r * 0.55f, cx, tipY);
            pin.closePath();
            g2.setPaint(FIconPaint.liquid(x, topY, w, tipY - topY));
            g2.fill(pin);
            FIconType.clipShine(g2, pin, x, topY, w, tipY - topY);
            FIconPaint.rim(g2, pin, Math.max(1.0f, d * 0.04f));
            float hr = w * 0.2f;
            Ellipse2D.Float hole = new Ellipse2D.Float(cx - hr, cyc - hr, 2.0f * hr, 2.0f * hr);
            g2.setColor(FIconPaint.DARK_HOLE);
            g2.fill(hole);
        }
    }
    ,
    CLOSE("Schlie\u00dfen"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim;
            float pad = d * 0.1f;
            float sz = d - 2.0f * pad;
            float cx = d / 2.0f;
            float cy = d / 2.0f;
            Ellipse2D.Float circ = new Ellipse2D.Float(pad, pad, sz, sz);
            g2.setPaint(new GradientPaint(pad, pad, FIconPaint.WINE_RED, pad + sz, pad + sz, new Color(77, 0, 16)));
            g2.fill(circ);
            FIconType.clipShine(g2, circ, pad, pad, sz, sz);
            FIconPaint.rim(g2, circ, Math.max(1.0f, d * 0.045f));
            g2.setColor(FIconPaint.INK);
            g2.setStroke(FIconType.round(d * 0.11f));
            float a = sz * 0.26f;
            g2.draw(new Line2D.Float(cx - a, cy - a, cx + a, cy + a));
            g2.draw(new Line2D.Float(cx - a, cy + a, cx + a, cy - a));
        }
    }
    ,
    MENU("Men\u00fc"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim;
            float pad = d * 0.18f;
            float w = d - 2.0f * pad;
            float bh = Math.max(2.0f, d * 0.11f);
            float gap = (d - 2.0f * pad - 3.0f * bh) / 2.0f;
            g2.setPaint(FIconPaint.liquid(pad, pad, w, d - 2.0f * pad));
            for (int i = 0; i < 3; ++i) {
                float y = pad + (float)i * (bh + gap);
                g2.fill(new RoundRectangle2D.Float(pad, y, w, bh, bh, bh));
            }
            g2.setColor(FIconPaint.alpha(Color.WHITE, 90));
            g2.fill(new RoundRectangle2D.Float(pad, pad, w * 0.5f, Math.max(1.0f, bh * 0.5f), bh, bh));
        }
    }
    ,
    DOWNLOAD("Download"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            FIconType.arrow(g2, dim, true);
        }
    }
    ,
    UPLOAD("Upload"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            FIconType.arrow(g2, dim, false);
        }
    }
    ,
    MINIMIZE("Minimieren"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim;
            float cx = d / 2.0f;
            float hw = d * 0.28f;
            float y = d * 0.66f;
            float th = Math.max(2.0f, d * 0.11f);
            g2.setPaint(FIconPaint.liquid(cx - hw, y, hw * 2.0f, th));
            g2.fill(new RoundRectangle2D.Float(cx - hw, y - th / 2.0f, hw * 2.0f, th, th, th));
            g2.setColor(FIconPaint.alpha(Color.WHITE, 90));
            g2.fill(new RoundRectangle2D.Float(cx - hw, y - th / 2.0f, hw, Math.max(1.0f, th * 0.5f), th, th));
        }
    }
    ,
    MAXIMIZE("Maximieren"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim;
            float pad = d * 0.2f;
            float sz = d - 2.0f * pad;
            RoundRectangle2D.Float box = new RoundRectangle2D.Float(pad, pad, sz, sz, d * 0.1f, d * 0.1f);
            g2.setPaint(FIconPaint.liquid(pad, pad, sz, sz));
            g2.fill(box);
            FIconType.clipShine(g2, box, pad, pad, sz, sz);
            FIconPaint.rim(g2, box, Math.max(1.0f, d * 0.045f));
            g2.setColor(FIconPaint.DARK_HOLE);
            float ip = pad + Math.max(1.5f, d * 0.09f);
            float isz = sz - 2.0f * Math.max(1.5f, d * 0.09f);
            g2.fill(new RoundRectangle2D.Float(ip, ip + d * 0.02f, isz, isz - d * 0.02f, d * 0.06f, d * 0.06f));
        }
    }
    ,
    RESTORE("Wiederherstellen"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim;
            float sz = d * 0.44f;
            float off = d * 0.12f;
            float frontX = d * 0.18f;
            float frontY = d * 0.3f;
            RoundRectangle2D.Float back = new RoundRectangle2D.Float(frontX + off, frontY - off, sz, sz, d * 0.09f, d * 0.09f);
            g2.setColor(FIconPaint.alpha(FIconPaint.TURQUOISE, 150));
            g2.fill(back);
            FIconPaint.rim(g2, back, Math.max(1.0f, d * 0.04f));
            RoundRectangle2D.Float front = new RoundRectangle2D.Float(frontX, frontY, sz, sz, d * 0.09f, d * 0.09f);
            g2.setPaint(FIconPaint.liquid(frontX, frontY, sz, sz));
            g2.fill(front);
            FIconType.clipShine(g2, front, frontX, frontY, sz, sz);
            FIconPaint.rim(g2, front, Math.max(1.0f, d * 0.045f));
            g2.setColor(FIconPaint.DARK_HOLE);
            float ip = Math.max(1.5f, d * 0.08f);
            g2.fill(new RoundRectangle2D.Float(frontX + ip, frontY + ip + d * 0.02f, sz - 2.0f * ip, sz - 2.0f * ip - d * 0.02f, d * 0.05f, d * 0.05f));
        }
    }
    ,
    CAT("Katze"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim;
            float cx = d / 2.0f;
            float hy = d * 0.3f;
            float hw = d * 0.62f;
            float hh = d * 0.5f;
            RoundRectangle2D.Float head = new RoundRectangle2D.Float(cx - hw / 2.0f, hy, hw, hh, hw * 0.5f, hh * 0.7f);
            Path2D.Float earL = new Path2D.Float();
            ((Path2D)earL).moveTo(cx - hw * 0.42f, hy + hh * 0.1f);
            ((Path2D)earL).lineTo(cx - hw * 0.3f, hy - hh * 0.34f);
            ((Path2D)earL).lineTo(cx - hw * 0.04f, hy + hh * 0.06f);
            earL.closePath();
            Path2D.Float earR = new Path2D.Float();
            ((Path2D)earR).moveTo(cx + hw * 0.42f, hy + hh * 0.1f);
            ((Path2D)earR).lineTo(cx + hw * 0.3f, hy - hh * 0.34f);
            ((Path2D)earR).lineTo(cx + hw * 0.04f, hy + hh * 0.06f);
            earR.closePath();
            g2.setPaint(FIconPaint.liquidDiagonal(cx - hw / 2.0f, hy - hh * 0.34f, cx + hw / 2.0f, hy + hh));
            g2.fill(earL);
            g2.fill(earR);
            g2.fill(head);
            FIconType.clipShine(g2, head, cx - hw / 2.0f, hy, hw, hh);
            FIconPaint.rim(g2, head, Math.max(1.0f, d * 0.04f));
            g2.setColor(FIconPaint.INK);
            float er = Math.max(1.0f, d * 0.055f);
            float ey = hy + hh * 0.42f;
            float ex = hw * 0.2f;
            g2.fill(new Ellipse2D.Float(cx - ex - er, ey - er, 2.0f * er, 2.0f * er));
            g2.fill(new Ellipse2D.Float(cx + ex - er, ey - er, 2.0f * er, 2.0f * er));
            g2.setColor(FIconPaint.alpha(FIconPaint.WINE_RED, 235));
            float ny = hy + hh * 0.62f;
            float nw = d * 0.07f;
            Path2D.Float nose = new Path2D.Float();
            ((Path2D)nose).moveTo(cx - nw, ny);
            ((Path2D)nose).lineTo(cx + nw, ny);
            ((Path2D)nose).lineTo(cx, ny + nw);
            nose.closePath();
            g2.fill(nose);
            g2.setColor(FIconPaint.alpha(FIconPaint.INK, 200));
            g2.setStroke(FIconType.round(Math.max(1.0f, d * 0.03f)));
            float wy = ny + nw * 0.4f;
            g2.draw(new Line2D.Float(cx - hw * 0.1f, wy, cx - hw * 0.52f, wy - d * 0.03f));
            g2.draw(new Line2D.Float(cx - hw * 0.1f, wy + d * 0.03f, cx - hw * 0.5f, wy + d * 0.06f));
            g2.draw(new Line2D.Float(cx + hw * 0.1f, wy, cx + hw * 0.52f, wy - d * 0.03f));
            g2.draw(new Line2D.Float(cx + hw * 0.1f, wy + d * 0.03f, cx + hw * 0.5f, wy + d * 0.06f));
        }
    }
    ,
    BEACH("Strand"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim, cx = d / 2.0f;
            float domeY = d * 0.20f, domeR = d * 0.32f;
            Path2D.Float dome = new Path2D.Float();
            dome.append(new Arc2D.Float(cx - domeR, domeY - domeR, 2 * domeR, 2 * domeR, 0, 180, Arc2D.CHORD), false);
            dome.closePath();
            g2.setPaint(FIconPaint.liquid(cx - domeR, domeY - domeR, 2 * domeR, 2 * domeR));
            g2.fill(dome);
            FIconType.clipShine(g2, dome, cx - domeR, domeY - domeR, 2 * domeR, 2 * domeR);
            FIconPaint.rim(g2, dome, Math.max(1.0f, d * 0.04f));
            g2.setColor(FIconPaint.INK);
            g2.setStroke(FIconType.round(Math.max(1.0f, d * 0.08f)));
            g2.draw(new Line2D.Float(cx, domeY, cx, d * 0.82f));
            Path2D.Float wave = new Path2D.Float();
            float wy = d * 0.86f, amp = d * 0.045f;
            wave.moveTo(d * 0.14f, wy);
            wave.curveTo(d * 0.28f, wy - amp, d * 0.36f, wy + amp, d * 0.5f, wy);
            wave.curveTo(d * 0.64f, wy - amp, d * 0.72f, wy + amp, d * 0.86f, wy);
            g2.setStroke(FIconType.round(Math.max(1.0f, d * 0.07f)));
            g2.draw(wave);
        }
    }
    ,
    RESTAURANT("Restaurant"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim, pad = d * 0.16f, sz = d - 2.0f * pad;
            Ellipse2D.Float plate = new Ellipse2D.Float(pad, pad, sz, sz);
            g2.setPaint(FIconPaint.liquid(pad, pad, sz, sz));
            g2.fill(plate);
            FIconType.clipShine(g2, plate, pad, pad, sz, sz);
            FIconPaint.rim(g2, plate, Math.max(1.0f, d * 0.045f));
            g2.setColor(FIconPaint.INK);
            float knifeX = d * 0.60f;
            Path2D.Float knife = new Path2D.Float();
            knife.moveTo(knifeX, d * 0.22f);
            knife.lineTo(knifeX + d * 0.05f, d * 0.22f);
            knife.lineTo(knifeX + d * 0.05f, d * 0.55f);
            knife.lineTo(knifeX + d * 0.02f, d * 0.78f);
            knife.lineTo(knifeX, d * 0.55f);
            knife.closePath();
            g2.fill(knife);
            float forkX = d * 0.36f;
            g2.setStroke(new BasicStroke(Math.max(1.0f, d * 0.035f)));
            for (int i = -1; i <= 1; ++i) {
                float x = forkX + i * d * 0.045f;
                g2.draw(new Line2D.Float(x, d * 0.22f, x, d * 0.40f));
            }
            Path2D.Float handle = new Path2D.Float();
            handle.moveTo(forkX - d * 0.045f, d * 0.40f);
            handle.lineTo(forkX + d * 0.045f, d * 0.40f);
            handle.lineTo(forkX + d * 0.02f, d * 0.78f);
            handle.lineTo(forkX - d * 0.02f, d * 0.78f);
            handle.closePath();
            g2.fill(handle);
        }
    }
    ,
    CHURCH("Kirche"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim, pad = d * 0.22f, w = d - 2.0f * pad;
            float bodyTop = d * 0.42f, bodyBottom = d * 0.84f;
            Path2D.Float tower = new Path2D.Float();
            tower.moveTo(d * 0.5f, d * 0.10f);
            tower.lineTo(pad + w, bodyTop);
            tower.lineTo(pad + w, bodyBottom);
            tower.lineTo(pad, bodyBottom);
            tower.lineTo(pad, bodyTop);
            tower.closePath();
            g2.setPaint(FIconPaint.liquid(pad, d * 0.10f, w, bodyBottom - d * 0.10f));
            g2.fill(tower);
            FIconType.clipShine(g2, tower, pad, d * 0.10f, w, bodyBottom - d * 0.10f);
            FIconPaint.rim(g2, tower, Math.max(1.0f, d * 0.04f));
            g2.setColor(FIconPaint.INK);
            g2.setStroke(FIconType.round(Math.max(1.0f, d * 0.06f)));
            float cx = d * 0.5f, crossTop = d * 0.02f, crossBottom = d * 0.16f;
            g2.draw(new Line2D.Float(cx, crossTop, cx, crossBottom));
            g2.draw(new Line2D.Float(cx - d * 0.06f, crossTop + d * 0.045f, cx + d * 0.06f, crossTop + d * 0.045f));
            g2.setColor(FIconPaint.DARK_HOLE);
            float doorW = w * 0.32f, doorX = d * 0.5f - doorW / 2.0f, doorTop = bodyBottom - d * 0.24f;
            Path2D.Float door = new Path2D.Float();
            door.moveTo(doorX, bodyBottom);
            door.lineTo(doorX, doorTop + doorW / 2.0f);
            door.quadTo(doorX, doorTop, doorX + doorW / 2.0f, doorTop);
            door.quadTo(doorX + doorW, doorTop, doorX + doorW, doorTop + doorW / 2.0f);
            door.lineTo(doorX + doorW, bodyBottom);
            door.closePath();
            g2.fill(door);
        }
    }
    ,
    BANK("Bank"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim, pad = d * 0.15f, w = d - 2.0f * pad;
            float baseY = d * 0.82f, roofY = d * 0.30f;
            Path2D.Float body = new Path2D.Float();
            body.moveTo(d * 0.5f, d * 0.14f);
            body.lineTo(pad, roofY);
            body.lineTo(pad + w, roofY);
            body.closePath();
            body.append(new Rectangle2D.Float(pad, roofY, w, baseY - roofY), false);
            g2.setPaint(FIconPaint.liquid(pad, d * 0.14f, w, baseY - d * 0.14f));
            g2.fill(body);
            FIconType.clipShine(g2, body, pad, d * 0.14f, w, baseY - d * 0.14f);
            FIconPaint.rim(g2, body, Math.max(1.0f, d * 0.04f));
            g2.setColor(FIconPaint.DARK_HOLE);
            int cols = 3;
            float colW = w * 0.12f, gap = (w - cols * colW) / (cols + 1);
            for (int i = 0; i < cols; ++i) {
                float x = pad + gap + i * (colW + gap);
                g2.fill(new Rectangle2D.Float(x, roofY + d * 0.06f, colW, baseY - roofY - d * 0.12f));
            }
            g2.setColor(FIconPaint.INK);
            g2.setStroke(FIconType.round(Math.max(1.0f, d * 0.06f)));
            g2.draw(new Line2D.Float(pad - d * 0.02f, baseY + d * 0.03f, pad + w + d * 0.02f, baseY + d * 0.03f));
        }
    }
    ,
    BAKERY("B\u00e4ckerei"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim, pad = d * 0.16f, sz = d - 2.0f * pad;
            Ellipse2D.Float roundel = new Ellipse2D.Float(pad, pad, sz, sz);
            g2.setPaint(FIconPaint.liquid(pad, pad, sz, sz));
            g2.fill(roundel);
            FIconType.clipShine(g2, roundel, pad, pad, sz, sz);
            FIconPaint.rim(g2, roundel, Math.max(1.0f, d * 0.045f));
            g2.setColor(FIconPaint.INK);
            g2.setStroke(FIconType.round(Math.max(1.0f, d * 0.085f)));
            Path2D.Float pretzel = new Path2D.Float();
            float cx = d * 0.5f, top = d * 0.30f, bottom = d * 0.72f;
            pretzel.moveTo(cx - d * 0.16f, top);
            pretzel.curveTo(cx - d * 0.32f, top + d * 0.08f, cx - d * 0.30f, bottom - d * 0.10f, cx, bottom);
            pretzel.curveTo(cx - d * 0.18f, top + d * 0.30f, cx + d * 0.18f, top + d * 0.30f, cx, bottom);
            pretzel.curveTo(cx + d * 0.30f, bottom - d * 0.10f, cx + d * 0.32f, top + d * 0.08f, cx + d * 0.16f, top);
            g2.draw(pretzel);
        }
    }
    ,
    SUPERMARKET("Supermarkt"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim, pad = d * 0.16f, sz = d - 2.0f * pad;
            Ellipse2D.Float badge = new Ellipse2D.Float(pad, pad, sz, sz);
            g2.setPaint(FIconPaint.liquid(pad, pad, sz, sz));
            g2.fill(badge);
            FIconType.clipShine(g2, badge, pad, pad, sz, sz);
            FIconPaint.rim(g2, badge, Math.max(1.0f, d * 0.045f));
            g2.setColor(FIconPaint.INK);
            g2.setStroke(FIconType.round(Math.max(1.0f, d * 0.06f)));
            Path2D.Float basket = new Path2D.Float();
            basket.moveTo(d * 0.30f, d * 0.34f);
            basket.lineTo(d * 0.68f, d * 0.34f);
            basket.lineTo(d * 0.62f, d * 0.58f);
            basket.lineTo(d * 0.36f, d * 0.58f);
            basket.closePath();
            g2.draw(basket);
            g2.draw(new Line2D.Float(d * 0.22f, d * 0.28f, d * 0.30f, d * 0.34f));
            g2.draw(new Line2D.Float(d * 0.20f, d * 0.24f, d * 0.26f, d * 0.24f));
            g2.fill(new Ellipse2D.Float(d * 0.38f, d * 0.63f, d * 0.07f, d * 0.07f));
            g2.fill(new Ellipse2D.Float(d * 0.56f, d * 0.63f, d * 0.07f, d * 0.07f));
        }
    }
    ,
    PARKING("Parkplatz"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim, pad = d * 0.14f, sz = d - 2.0f * pad;
            RoundRectangle2D.Float sign = new RoundRectangle2D.Float(pad, pad, sz, sz, sz * 0.22f, sz * 0.22f);
            g2.setPaint(FIconPaint.liquid(pad, pad, sz, sz));
            g2.fill(sign);
            FIconType.clipShine(g2, sign, pad, pad, sz, sz);
            FIconPaint.rim(g2, sign, Math.max(1.0f, d * 0.045f));
            g2.setColor(FIconPaint.INK);
            java.awt.Font f = new java.awt.Font("SansSerif", 1, Math.round(d * 0.56f));
            g2.setFont(f);
            java.awt.font.FontRenderContext frc = g2.getFontRenderContext();
            java.awt.font.GlyphVector gv = f.createGlyphVector(frc, "P");
            Rectangle2D bounds = gv.getVisualBounds();
            float x = (float)(d / 2.0 - bounds.getWidth() / 2.0 - bounds.getX());
            float y = (float)(d / 2.0 - bounds.getHeight() / 2.0 - bounds.getY());
            g2.drawGlyphVector(gv, x, y);
        }
    }
    ,
    BUS_STOP("Bushaltestelle"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim, pad = d * 0.18f, w = d - 2.0f * pad, h = d * 0.46f;
            float top = d * 0.22f;
            RoundRectangle2D.Float bus = new RoundRectangle2D.Float(pad, top, w, h, d * 0.10f, d * 0.10f);
            g2.setPaint(FIconPaint.liquid(pad, top, w, h));
            g2.fill(bus);
            FIconType.clipShine(g2, bus, pad, top, w, h);
            FIconPaint.rim(g2, bus, Math.max(1.0f, d * 0.04f));
            g2.setColor(FIconPaint.DARK_HOLE);
            float winW = w * 0.32f, winH = h * 0.42f, winY = top + h * 0.16f;
            g2.fill(new RoundRectangle2D.Float(pad + w * 0.10f, winY, winW, winH, d * 0.03f, d * 0.03f));
            g2.fill(new RoundRectangle2D.Float(pad + w * 0.56f, winY, winW, winH, d * 0.03f, d * 0.03f));
            g2.setColor(FIconPaint.INK);
            float wheelD = d * 0.14f, wheelY = top + h - wheelD * 0.5f;
            g2.fill(new Ellipse2D.Float(pad + w * 0.14f, wheelY, wheelD, wheelD));
            g2.fill(new Ellipse2D.Float(pad + w * 0.72f, wheelY, wheelD, wheelD));
        }
    }
    ,
    TOILET("Toilette"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim, pad = d * 0.16f, sz = d - 2.0f * pad;
            Ellipse2D.Float badge = new Ellipse2D.Float(pad, pad, sz, sz);
            g2.setPaint(FIconPaint.liquid(pad, pad, sz, sz));
            g2.fill(badge);
            FIconType.clipShine(g2, badge, pad, pad, sz, sz);
            FIconPaint.rim(g2, badge, Math.max(1.0f, d * 0.045f));
            g2.setColor(FIconPaint.INK);
            g2.setStroke(FIconType.round(Math.max(1.0f, d * 0.055f)));
            FIconType.toiletFigure(g2, d * 0.36f, d);
            FIconType.toiletFigure(g2, d * 0.64f, d);
        }
    }
    ,
    PARK("Park"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim, cx = d / 2.0f;
            float crownR = d * 0.30f, crownCy = d * 0.38f;
            Ellipse2D.Float crown = new Ellipse2D.Float(cx - crownR, crownCy - crownR, crownR * 2, crownR * 2);
            g2.setPaint(FIconPaint.liquid(cx - crownR, crownCy - crownR, crownR * 2, crownR * 2));
            g2.fill(crown);
            FIconType.clipShine(g2, crown, cx - crownR, crownCy - crownR, crownR * 2, crownR * 2);
            FIconPaint.rim(g2, crown, Math.max(1.0f, d * 0.045f));
            g2.setColor(FIconPaint.INK);
            float trunkW = d * 0.10f;
            g2.fill(new Rectangle2D.Float(cx - trunkW / 2.0f, crownCy + crownR * 0.55f, trunkW, d * 0.30f));
            g2.setStroke(FIconType.round(Math.max(1.0f, d * 0.05f)));
            g2.draw(new Line2D.Float(d * 0.24f, d * 0.86f, d * 0.76f, d * 0.86f));
        }
    }
    ,
    HARBOR("Hafen"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim, pad = d * 0.16f, sz = d - 2.0f * pad;
            Ellipse2D.Float badge = new Ellipse2D.Float(pad, pad, sz, sz);
            g2.setPaint(FIconPaint.liquid(pad, pad, sz, sz));
            g2.fill(badge);
            FIconType.clipShine(g2, badge, pad, pad, sz, sz);
            FIconPaint.rim(g2, badge, Math.max(1.0f, d * 0.045f));
            g2.setColor(FIconPaint.INK);
            g2.setStroke(FIconType.round(Math.max(1.0f, d * 0.075f)));
            float cx = d * 0.5f;
            float ringR = d * 0.075f, ringCy = d * 0.24f;
            g2.draw(new Ellipse2D.Float(cx - ringR, ringCy - ringR, ringR * 2, ringR * 2));
            g2.draw(new Line2D.Float(cx, ringCy + ringR, cx, d * 0.72f));
            g2.draw(new Line2D.Float(cx - d * 0.14f, d * 0.38f, cx + d * 0.14f, d * 0.38f));
            float armR = d * 0.20f;
            g2.draw(new Arc2D.Float(cx - armR, d * 0.72f - armR, armR * 2, armR * 2, 180, 90, Arc2D.OPEN));
            g2.draw(new Arc2D.Float(cx - armR, d * 0.72f - armR, armR * 2, armR * 2, 270, 90, Arc2D.OPEN));
        }
    }
    ,
    SHOP("Gesch\u00e4ft"){

        @Override
        public void paintGlyph(Graphics2D g2, int dim) {
            float d = dim, pad = d * 0.20f, w = d - 2.0f * pad, top = d * 0.36f, h = d * 0.46f;
            RoundRectangle2D.Float bag = new RoundRectangle2D.Float(pad, top, w, h, d * 0.06f, d * 0.06f);
            g2.setPaint(FIconPaint.liquid(pad, top, w, h));
            g2.fill(bag);
            FIconType.clipShine(g2, bag, pad, top, w, h);
            FIconPaint.rim(g2, bag, Math.max(1.0f, d * 0.04f));
            g2.setColor(FIconPaint.INK);
            g2.setStroke(FIconType.round(Math.max(1.0f, d * 0.055f)));
            float handleR = w * 0.28f;
            g2.draw(new Arc2D.Float(d * 0.5f - handleR, top - handleR * 1.3f, handleR * 2, handleR * 1.6f, 0, 180, Arc2D.OPEN));
            g2.fillOval((int)(d * 0.5f - d * 0.035f), (int)(top + h * 0.35f), (int)(d * 0.07f), (int)(d * 0.07f));
        }
    };

    private final String label;

    private FIconType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return this.label;
    }

    public static FIconType find(String name) {
        if (name == null) {
            return null;
        }
        for (FIconType t : FIconType.values()) {
            if (!t.name().equalsIgnoreCase(name)) continue;
            return t;
        }
        return null;
    }

    private static BasicStroke round(float w) {
        return new BasicStroke(w, 1, 1);
    }

    private static void clipShine(Graphics2D g2, Shape shape, float x, float y, float w, float h) {
        Shape oc = g2.getClip();
        g2.clip(shape);
        FIconPaint.shine(g2, x, y, w, h);
        g2.setClip(oc);
    }

    private static void toiletFigure(Graphics2D g2, float cx, float d) {
        float headR = d * 0.06f;
        g2.fill(new Ellipse2D.Float(cx - headR, d * 0.30f, headR * 2.0f, headR * 2.0f));
        g2.draw(new Line2D.Float(cx, d * 0.30f + headR * 2.0f, cx, d * 0.62f));
        g2.draw(new Line2D.Float(cx - d * 0.07f, d * 0.42f, cx + d * 0.07f, d * 0.42f));
        g2.draw(new Line2D.Float(cx, d * 0.62f, cx - d * 0.06f, d * 0.76f));
        g2.draw(new Line2D.Float(cx, d * 0.62f, cx + d * 0.06f, d * 0.76f));
    }

    private static void arrow(Graphics2D g2, int dim, boolean down) {
        float tipY;
        float baseY;
        float shaftBot;
        float shaftTop;
        float headBaseY;
        float d = dim;
        float cx = d / 2.0f;
        float shaftW = d * 0.15f;
        float headHalf = d * 0.19f;
        float headLen = d * 0.2f;
        float aTop = d * 0.15f;
        float aBot = d * 0.62f;
        float f = headBaseY = down ? aBot : d * 0.74f - (aBot - aTop);
        if (down) {
            shaftTop = aTop;
            baseY = shaftBot = d * 0.52f;
            tipY = baseY + headLen;
        } else {
            tipY = d * 0.14f;
            shaftTop = baseY = tipY + headLen;
            shaftBot = d * 0.66f;
        }
        g2.setPaint(FIconPaint.liquid(cx - shaftW, aTop, shaftW * 2.0f, d * 0.7f));
        g2.fill(new RoundRectangle2D.Float(cx - shaftW / 2.0f, shaftTop, shaftW, shaftBot - shaftTop, shaftW, shaftW));
        Path2D.Float head = new Path2D.Float();
        ((Path2D)head).moveTo(cx - headHalf, baseY);
        ((Path2D)head).lineTo(cx + headHalf, baseY);
        ((Path2D)head).lineTo(cx, tipY);
        head.closePath();
        g2.fill(head);
        g2.setColor(FIconPaint.alpha(FIconPaint.INK, 215));
        g2.setStroke(new BasicStroke(d * 0.075f, 1, 1));
        float ty = d * 0.74f;
        float by = d * 0.86f;
        float hw = d * 0.27f;
        Path2D.Float tray = new Path2D.Float();
        ((Path2D)tray).moveTo(cx - hw, ty);
        ((Path2D)tray).lineTo(cx - hw, by);
        ((Path2D)tray).lineTo(cx + hw, by);
        ((Path2D)tray).lineTo(cx + hw, ty);
        g2.draw(tray);
    }
}

