package com.example.chargedparticles.distributed;

import com.example.chargedparticles.model.Particle;
import com.example.chargedparticles.simulation.ForceUtils;
import com.example.chargedparticles.simulation.SimulationParameters;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementacija delavskega vozlišča za porazdeljeno simulacijo.
 * Vsako vozlišče je odgovorno za računanje sil in posodabljanje pozicij
 * za svoj del delcev.
 */
public class WorkerNodeImpl extends UnicastRemoteObject implements WorkerNode {
    
    private final int workerId;
    private final AtomicBoolean active = new AtomicBoolean(true);
    
    // Delci, za katere je odgovorno to vozlišče
    private List<ParticleState> assignedParticles = new ArrayList<>();
    private SimulationParameters parameters;
    
    // Preračunani delci z novimi hitrostmi in pozicijami
    private List<ParticleState> updatedParticles = new ArrayList<>();
    
    public WorkerNodeImpl(int workerId) throws RemoteException {
        super();
        this.workerId = workerId;
        System.out.println("Delavsko vozlišče " + workerId + " inicializirano");
    }
    
    @Override
    public void initialize(List<ParticleState> assignedParticles, List<ParticleState> allParticles, 
                          SimulationParameters params) throws RemoteException {
        if (!active.get()) {
            throw new RemoteException("Vozlišče ni več aktivno");
        }
        
        this.assignedParticles = new ArrayList<>(assignedParticles);
        this.parameters = params;
        this.updatedParticles = new ArrayList<>();
        
        System.out.println("Vozlišče " + workerId + " inicializirano z " + 
                         assignedParticles.size() + " delci");
    }
    
    @Override
    public List<ParticleState> calculateForces(List<ParticleState> allParticles) throws RemoteException {
        if (!active.get()) {
            throw new RemoteException("Vozlišče ni več aktivno");
        }
        
        List<ParticleState> updatedParticles = new ArrayList<>();
        
        // Za vsak dodeljeni delec izračunaj sile in posodobi hitrost
        for (ParticleState myParticle : assignedParticles) {
            // Pretvori ParticleState v Particle za uporabo obstoječih metod
            Particle p = createParticleFromState(myParticle);
            
            double totalForceX = 0.0;
            double totalForceY = 0.0;
            
            // Izračunaj sile z vsemi ostalimi delci
            for (ParticleState otherState : allParticles) {
                if (otherState.getId() != myParticle.getId()) {
                    Particle other = createParticleFromState(otherState);
                    double[] force = ForceUtils.computeParticleForce(p, other);
                    totalForceX += force[0];
                    totalForceY += force[1];
                }
            }
            
            // Dodaj mejne sile
            double[] boundaryForce = ForceUtils.computeBoundaryForce(p, parameters);
            totalForceX += boundaryForce[0];
            totalForceY += boundaryForce[1];
            
            // Posodobi hitrost (F = ma, torej a = F/m, v_new = v_old + a)
            double mass = myParticle.getMass();
            double newVx = myParticle.getVx() + (totalForceX / mass);
            double newVy = myParticle.getVy() + (totalForceY / mass);
            
            // Ustvari posodobljen delec z novo hitrostjo
            ParticleState updated = new ParticleState(
                myParticle.getId(),
                myParticle.getX(), 
                myParticle.getY(),
                newVx, 
                newVy,
                myParticle.getCharge(),
                myParticle.getMass()
            );
            
            updatedParticles.add(updated);
        }
        
        // Shrani za kasnejše posodabljanje pozicij
        this.updatedParticles = updatedParticles;
        
        return updatedParticles;
    }
    
    @Override
    public List<ParticleState> updatePositions() throws RemoteException {
        if (!active.get()) {
            throw new RemoteException("Vozlišče ni več aktivno");
        }
        
        List<ParticleState> finalParticles = new ArrayList<>();
        
        // Posodobi pozicije na osnovi novih hitrosti
        for (ParticleState particle : updatedParticles) {
            double newX = particle.getX() + particle.getVx();
            double newY = particle.getY() + particle.getVy();
            
            ParticleState updated = new ParticleState(
                particle.getId(),
                newX, 
                newY,
                particle.getVx(), 
                particle.getVy(),
                particle.getCharge(),
                particle.getMass()
            );
            
            finalParticles.add(updated);
        }
        
        // Posodobi lokalno stanje
        this.assignedParticles = finalParticles;
        
        return finalParticles;
    }
    
    @Override
    public String getWorkerInfo() throws RemoteException {
        return String.format("Delavsko vozlišče %d: %d delcev, aktivno: %s", 
                           workerId, assignedParticles.size(), active.get());
    }
    
    @Override
    public boolean isAlive() throws RemoteException {
        return active.get();
    }
    
    @Override
    public void shutdown() throws RemoteException {
        active.set(false);
        assignedParticles.clear();
        updatedParticles.clear();
        System.out.println("Vozlišče " + workerId + " označeno kot neaktivno");
    }
    
    @Override
    public void reset() throws RemoteException {
        assignedParticles.clear();
        updatedParticles.clear();
        System.out.println("Vozlišče " + workerId + " ponastavljeno za novo simulacijo");
    }
    
    @Override
    public List<ParticleState> calculateAndUpdate(List<ParticleState> allParticles) throws RemoteException {
        if (!active.get()) {
            throw new RemoteException("Vozlišče ni več aktivno");
        }
        
        List<ParticleState> finalParticles = new ArrayList<>();
        
        // Za vsak dodeljeni delec: izračunaj sile, posodobi hitrost IN pozicijo
        for (ParticleState myParticle : assignedParticles) {
            // Pretvori ParticleState v Particle za uporabo obstoječih metod
            Particle p = createParticleFromState(myParticle);
            
            double totalForceX = 0.0;
            double totalForceY = 0.0;
            
            // Izračunaj sile z vsemi ostalimi delci
            for (ParticleState otherState : allParticles) {
                if (otherState.getId() != myParticle.getId()) {
                    Particle other = createParticleFromState(otherState);
                    double[] force = ForceUtils.computeParticleForce(p, other);
                    totalForceX += force[0];
                    totalForceY += force[1];
                }
            }
            
            // Dodaj mejne sile
            double[] boundaryForce = ForceUtils.computeBoundaryForce(p, parameters);
            totalForceX += boundaryForce[0];
            totalForceY += boundaryForce[1];
            
            // Posodobi hitrost (F = ma, torej a = F/m, v_new = v_old + a)
            double mass = myParticle.getMass();
            double newVx = myParticle.getVx() + (totalForceX / mass);
            double newVy = myParticle.getVy() + (totalForceY / mass);
            
            // Posodobi tudi pozicijo
            double newX = myParticle.getX() + newVx;
            double newY = myParticle.getY() + newVy;
            
            // Ustvari popolnoma posodobljen delec
            ParticleState updated = new ParticleState(
                myParticle.getId(),
                newX, 
                newY,
                newVx, 
                newVy,
                myParticle.getCharge(),
                myParticle.getMass()
            );
            
            finalParticles.add(updated);
        }
        
        // Posodobi lokalno stanje
        this.assignedParticles = finalParticles;
        
        return finalParticles;
    }
    
    /**
     * Pomožna metoda za pretvorbo ParticleState v Particle.
     * Potrebna za uporabo obstoječih metod ForceUtils.
     */
    private Particle createParticleFromState(ParticleState state) {
        Particle p = new Particle(state.getX(), state.getY(), state.getVx(), state.getVy(), state.getCharge());
        return p;
    }
    
    public int getWorkerId() {
        return workerId;
    }
}