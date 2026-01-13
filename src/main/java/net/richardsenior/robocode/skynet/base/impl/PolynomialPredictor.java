package net.richardsenior.robocode.skynet.base.impl;
import net.richardsenior.robocode.skynet.base.Predictor;
import net.richardsenior.robocode.skynet.base.EventWrapper;
import java.util.Queue;
import java.awt.geom.Point2D;

/** Implementation of predictor backed by polynomials */
public class PolynomialPredictor implements Predictor {
    private double[] xCoeffs;
    private double[] yCoeffs;
    
    public PolynomialPredictor(Queue<? extends EventWrapper>scanHistory) {
        if (scanHistory.size() < 3) {
            xCoeffs = yCoeffs = null;
            return;
        }
        
        // Extract data from scan history
        double[] times = new double[scanHistory.size()];
        double[] xCoords = new double[scanHistory.size()];
        double[] yCoords = new double[scanHistory.size()];
        
        int i = 0;
        for (EventWrapper scan : scanHistory) {
            times[i] = scan.getTime();
            xCoords[i] = scan.getPosition().x;
            yCoords[i] = scan.getPosition().y;
            i++;
        }
        
        // Normalize time
        double baseTime = times[0];
        for (int j = 0; j < times.length; j++) {
            times[j] -= baseTime;
        }
        
        // Fit polynomials
        xCoeffs = fitQuadratic(times, xCoords);
        yCoeffs = fitQuadratic(times, yCoords);
    }
    
    @Override
    public Point2D.Double predict(int ticks) {
        if (xCoeffs == null || yCoeffs == null) return null;
        
        double x = evaluatePolynomial(xCoeffs, ticks);
        double y = evaluatePolynomial(yCoeffs, ticks);
        return new Point2D.Double(x, y);
    }
    
    @Override
    public Point2D.Double getIntercept(double bulletVelocity, Point2D.Double firingPosition) {
        if (xCoeffs == null || yCoeffs == null) return null;
        
        // Get current position and velocity (linear approximation)
        Point2D.Double currentPos = predict(0);
        Point2D.Double nextPos = predict(1);
        double vx = nextPos.x - currentPos.x;
        double vy = nextPos.y - currentPos.y;
        
        // Solve quadratic: |pos + vel*t - firing| = bulletVel*t
        double dx = currentPos.x - firingPosition.x;
        double dy = currentPos.y - firingPosition.y;
        
        double a = vx*vx + vy*vy - bulletVelocity*bulletVelocity;
        double b = 2*(dx*vx + dy*vy);
        double c = dx*dx + dy*dy;
        
        double discriminant = b*b - 4*a*c;
        if (discriminant < 0 || Math.abs(a) < 1e-6) return currentPos; // fallback
        
        double t = (-b + Math.sqrt(discriminant)) / (2*a);
        if (t < 0) t = (-b - Math.sqrt(discriminant)) / (2*a);
        if (t < 0) return currentPos; // fallback
        
        return new Point2D.Double(currentPos.x + vx*t, currentPos.y + vy*t);
    }

    private double evaluatePolynomial(double[] coeffs, double t) {
        double result = 0;
        double tPower = 1;
        for (double coeff : coeffs) {
            result += coeff * tPower;
            tPower *= t;
        }
        return result;
    }
    
    private double[] fitQuadratic(double[] x, double[] y) {
        int n = x.length;
        
        double sumX = 0, sumX2 = 0, sumX3 = 0, sumX4 = 0;
        double sumY = 0, sumXY = 0, sumX2Y = 0;
        
        for (int i = 0; i < n; i++) {
            double xi = x[i];
            double xi2 = xi * xi;
            sumX += xi;
            sumX2 += xi2;
            sumX3 += xi2 * xi;
            sumX4 += xi2 * xi2;
            sumY += y[i];
            sumXY += xi * y[i];
            sumX2Y += xi2 * y[i];
        }
        
        double det = n * (sumX2 * sumX4 - sumX3 * sumX3) - 
                    sumX * (sumX * sumX4 - sumX2 * sumX3) + 
                    sumX2 * (sumX * sumX3 - sumX2 * sumX2);
        
        if (Math.abs(det) < 1e-10) {
            double slope = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
            double intercept = (sumY - slope * sumX) / n;
            return new double[]{intercept, slope};
        }
        
        double a = (sumY * (sumX2 * sumX4 - sumX3 * sumX3) - 
                   sumXY * (sumX * sumX4 - sumX2 * sumX3) + 
                   sumX2Y * (sumX * sumX3 - sumX2 * sumX2)) / det;
        
        double b = (n * (sumXY * sumX4 - sumX2Y * sumX3) - 
                   sumY * (sumX * sumX4 - sumX2 * sumX3) + 
                   sumX2Y * (sumX * sumX2 - n * sumX3)) / det;
        
        double c = (n * (sumX2 * sumX2Y - sumX3 * sumXY) - 
                   sumX * (sumX * sumX2Y - sumX2 * sumXY) + 
                   sumY * (sumX * sumX3 - sumX2 * sumX2)) / det;
        
        return new double[]{a, b, c};
    }
}