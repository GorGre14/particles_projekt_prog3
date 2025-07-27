package com.example.chargedparticles.distributed;

import com.example.chargedparticles.simulation.SimulationParameters;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * RMI vmesnik za delavsko vozlišče v porazdeljeni simulaciji.
 * Definira operacije, ki jih lahko master izvaja na delavskih vozliščih.
 */
public interface WorkerNode extends Remote {
    
    /**
     * Inicializira delavsko vozlišče z določenimi delci.
     * @param assignedParticles Seznam delcev, za katere je odgovorno to vozlišče
     * @param allParticles Vsi delci v simulaciji (potrebni za izračun sil)
     * @param params Parametri simulacije
     * @throws RemoteException če pride do napake pri komunikaciji
     */
    void initialize(List<ParticleState> assignedParticles, List<ParticleState> allParticles, 
                   SimulationParameters params) throws RemoteException;
    
    /**
     * Izračuna sile in posodobi hitrosti za dodeljene delce.
     * @param allParticles Trenutno stanje vseh delcev
     * @return Seznam posodobljenih delcev z novimi hitrostmi
     * @throws RemoteException če pride do napake pri komunikaciji
     */
    List<ParticleState> calculateForces(List<ParticleState> allParticles) throws RemoteException;
    
    /**
     * Posodobi pozicije delcev na podlagi trenutnih hitrosti.
     * @return Seznam delcev z posodobljenimi pozicijami
     * @throws RemoteException če pride do napake pri komunikaciji
     */
    List<ParticleState> updatePositions() throws RemoteException;
    
    /**
     * Kombinirani izračun sil in posodobitev pozicij v enem klicu.
     * To zmanjša število omrežnih klicev in izboljša učinkovitost.
     * @param allParticles Trenutno stanje vseh delcev
     * @return Seznam popolnoma posodobljenih delcev
     * @throws RemoteException če pride do napake pri komunikaciji
     */
    List<ParticleState> calculateAndUpdate(List<ParticleState> allParticles) throws RemoteException;
    
    /**
     * Vrne informacije o delavskem vozlišču.
     * @return Informativni niz o vozlišču
     * @throws RemoteException če pride do napake pri komunikaciji
     */
    String getWorkerInfo() throws RemoteException;
    
    /**
     * Preveri, ali je vozlišče še vedno aktivno.
     * @return true če je vozlišče aktivno
     * @throws RemoteException če pride do napake pri komunikaciji
     */
    boolean isAlive() throws RemoteException;
    
    /**
     * Zaustavitev vozlišča.
     * @throws RemoteException če pride do napake pri komunikaciji
     */
    void shutdown() throws RemoteException;
    
    /**
     * Ponastavi stanje delavskega vozlišča za novo simulacijo.
     * @throws RemoteException če pride do napake pri komunikaciji
     */
    void reset() throws RemoteException;
}