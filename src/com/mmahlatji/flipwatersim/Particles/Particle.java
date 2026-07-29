package com.mmahlatji.flipwatersim.Particles;

import com.mmahlatji.flipwatersim.Solver.Vector2D;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
public class Particle {

    private final Vector2D position;
    private final Vector2D velocity;
    private final double radius, mass;


    public Particle(Vector2D position, Vector2D velocity, double radius, double mass) {
        this.position  = new Vector2D(position);
        this.velocity  = new Vector2D(velocity);
        this.radius  = radius;
        this.mass    = mass;
    }

    public Particle(final Particle particle) {
        this.position = new Vector2D(particle.getPosition());
        this.velocity = new Vector2D(particle.getVelocity());
        this.radius = particle.getRadius();
        this.mass = particle.getMass();
    }

    public Vector2D getPosition() {
        return new Vector2D(position);
    }

    public Vector2D getVelocity() {
        return new Vector2D(velocity);
    }


    public double getRadius() {
        return radius;
    }

    public double getMass() {
        return mass;
    }

    public void setPosition(Vector2D position) {
        this.position.setPoints(position.getX(), position.getY());
    }


    public void setVelocity(Vector2D velocity) {
        this.velocity.setPoints(velocity.getX(), velocity.getY());
    }


    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.WHITE);
        g2.fillOval(
            (int) (position.getX() - radius),
            (int) (position.getY() - radius),
            (int) radius * 2, 
            (int) radius * 2
            );
    }
    
}
