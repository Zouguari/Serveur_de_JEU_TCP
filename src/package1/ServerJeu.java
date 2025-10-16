package package1;

import java.io.*;
import java.net.*;
import java.util.Random;

/**
 * 
 * Objectif : 
 * Créer un serveur de jeu où plusieurs clients devinent un nombre secret.
 * Le premier qui trouve gagne et le jeu se termine pour tous.
 * 
 * @author ENSIASD - TP1 Systèmes d'Information Distribués
 */
public class ServerJeu extends Thread {
    
    // Variables partagées entre tous les threads
    private int nombreSecret;           // Nombre à deviner (0-1000)
    private int nbClient = 0;           // Compteur de clients connectés
    private boolean finJeux = false;    // Flag indiquant si le jeu est terminé
    private String gagnant = "";        // IP du client gagnant
    
    /**     */
    @Override
    public void run() {
        try {
            // ÉTAPE 1 : Créer le ServerSocket sur le port 1234
            ServerSocket serverSocket = new ServerSocket(1234);
            System.out.println("╔════════════════════════════════════════════════╗");
            System.out.println("║    SERVEUR DE JEU DÉMARRÉ SUR PORT 1234        ║");
            System.out.println("╚════════════════════════════════════════════════╝");
            
            // ÉTAPE 2 : Générer un nombre secret aléatoire entre 0 et 1000
            nombreSecret = new Random().nextInt(1001);
            System.out.println("\n[SERVEUR] Nombre secret généré : " + nombreSecret);
            System.out.println("[SERVEUR] En attente de connexions...\n");
            
            // ÉTAPE 3 : Boucle infinie pour accepter les connexions
            while (true) {
                // Attendre qu'un client se connecte (bloquant)
                Socket socket = serverSocket.accept();
                
                // ÉTAPE 4 : Incrémenter le compteur de clients
                nbClient++;
                
                // Afficher les informations de connexion
                String clientIP = socket.getRemoteSocketAddress().toString();
                System.out.println("→ Client #" + nbClient + " connecté depuis : " + clientIP);
                
                // ÉTAPE 4 : Créer un thread Conversation pour gérer ce client
                Conversation conv = new Conversation(socket, nbClient);
                conv.start();
            }
            
        } catch (IOException e) {
            System.err.println("❌ Erreur serveur : " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * CLASSE INTERNE : Conversation
     * Gère la communication avec UN client dans un thread séparé
     */
    class Conversation extends Thread {
        private Socket socketClient;
        private int numeroClient;
        
        /**
         * Constructeur
         * @param socket Socket de connexion avec le client
         * @param num Numéro du client
         */
        public Conversation(Socket socket, int num) {
            this.socketClient = socket;
            this.numeroClient = num;
        }
        
        @Override
        public void run() {
            BufferedReader br = null;
            PrintWriter pw = null;
            String clientIP = socketClient.getRemoteSocketAddress().toString();
            
            try {
                // ÉTAPE 6 : Créer les flux d'entrée/sortie
                br = new BufferedReader(
                    new InputStreamReader(socketClient.getInputStream())
                );
                pw = new PrintWriter(socketClient.getOutputStream(), true);
                
                // ÉTAPE 7 : Envoyer le message de bienvenue
                pw.println("╔═══════════════════════════════════════════╗");
                pw.println("║   BIENVENUE AU JEU DU NOMBRE SECRET !    ║");
                pw.println("╚═══════════════════════════════════════════╝");
                pw.println("");
                pw.println("✓ Vous êtes le client n°" + numeroClient);
                pw.println("✓ Devinez le nombre secret entre 0 et 1000 !");
                pw.println("───────────────────────────────────────────");
                
                System.out.println("  [Client #" + numeroClient + "] Bienvenue envoyé");
                
                // Boucle de jeu : tant que le jeu n'est pas terminé
                while (!finJeux) {
                    // Inviter le client à proposer un nombre
                    pw.println("\n→ Proposez un nombre : ");
                    
                    // ÉTAPE 8 : Lire la réponse du client
                    String reponse = br.readLine();
                    
                    // Vérifier si le client s'est déconnecté
                    if (reponse == null) {
                        System.out.println("  [Client #" + numeroClient + "] Déconnecté");
                        break;
                    }
                    
                    // ÉTAPE 9 : Convertir en entier avec gestion d'erreur
                    try {
                        int nombre = Integer.parseInt(reponse.trim());
                        
                        System.out.println("  [Client #" + numeroClient + "] Propose : " + nombre);
                        
                        // Vérifier à nouveau si le jeu est terminé
                        // (un autre thread pourrait avoir gagné entre temps)
                        if (finJeux) {
                            pw.println("\n╔═══════════════════════════════════════════╗");
                            pw.println("║          LE JEU EST TERMINÉ !             ║");
                            pw.println("╚═══════════════════════════════════════════╝");
                            pw.println("Le gagnant est : " + gagnant);
                            pw.println("Le nombre secret était : " + nombreSecret);
                            break;
                        }
                        
                        // ÉTAPE 10 : Comparer avec le nombre secret
                        if (nombre < nombreSecret) {
                            pw.println("↑ Votre nombre est TROP PETIT !");
                            
                        } else if (nombre > nombreSecret) {
                            pw.println("↓ Votre nombre est TROP GRAND !");
                            
                        } else {
                            // ÉTAPE 11 : Le client a trouvé le bon nombre !
                            // Marquer le jeu comme terminé
                            finJeux = true;
                            gagnant = clientIP;
                            
                            // Envoyer le message de victoire
                            pw.println("\n╔═══════════════════════════════════════════╗");
                            pw.println("║    🎉 SUPER !! VOUS AVEZ GAGNÉ ! 🎉      ║");
                            pw.println("╚═══════════════════════════════════════════╝");
                            pw.println("Le nombre secret était bien : " + nombreSecret);
                            pw.println("Félicitations !");
                            
                            // Log serveur
                            System.out.println("\n★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★");
                            System.out.println("★  CLIENT #" + numeroClient + " A GAGNÉ !");
                            System.out.println("★  Nombre secret : " + nombreSecret);
                            System.out.println("★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★\n");
                        }
                        
                    } catch (NumberFormatException e) {
                        // ÉTAPE 9 : Gestion des erreurs de format
                        pw.println("⚠️  ERREUR : Veuillez entrer un NOMBRE valide !");
                        System.out.println("[Client #" + numeroClient + "] Valeur invalide : " + reponse);
                    }
                }
                
                // Si le jeu est terminé et que ce client n'est pas le gagnant
                if (finJeux && !clientIP.equals(gagnant)) {
                    pw.println("\n╔═══════════════════════════════════════════╗");
                    pw.println("║          LE JEU EST TERMINÉ !             ║");
                    pw.println("╚═══════════════════════════════════════════╝");
                    pw.println("Le gagnant est : " + gagnant);
                    pw.println("Le nombre secret était : " + nombreSecret);
                    pw.println("\nMerci d'avoir participé !");
                }
                
            } catch (IOException e) {
                System.err.println("❌ Erreur avec client #" + numeroClient + " : " + e.getMessage());
                
            } finally {
                // Nettoyage : fermer toutes les ressources
                System.out.println("[Client #" + numeroClient + "] Déconnexion - Nettoyage des ressources");
                try {
                    if (br != null) br.close();
                    if (pw != null) pw.close();
                    if (socketClient != null) socketClient.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * POINT D'ENTRÉE DU PROGRAMME
     */
    public static void main(String[] args) {
        System.out.println("\n╔════════════════════════════════════════════════╗");
        System.out.println("║      SERVEUR DE JEU - NOMBRE SECRET           ║");
        System.out.println("║              TP1 - ENSIASD                     ║");
        System.out.println("║         Systèmes d'Information Distribués      ║");
        System.out.println("╚════════════════════════════════════════════════╝\n");
        
        // Démarrer le serveur dans un thread
        new ServerJeu().start();
    }
}