package com.github.vevc.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author vevc
 */
@Slf4j
@UtilityClass
public class TlsCertGenerator {

    private static final String KEY_FILE_NAME = "key.pem";
    private static final String CERT_FILE_NAME = "cert.pem";

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Generates a self-signed X.509 certificate and a private key.
     *
     * @param commonName The Common Name (CN) for the certificate's subject.
     * @param days       The validity period of the certificate in days.
     * @param keySize    The size of the RSA key to be generated.
     * @param certPath   The directory where the certificate and private key files will be saved.
     * @throws Exception if any error occurs during generation.
     */
    public void generate(String commonName, int days, int keySize, File certPath) throws Exception {
        // 1. Generate RSA Key Pair
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
        keyPairGenerator.initialize(keySize, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        // 2. Define Certificate Details
        long now = System.currentTimeMillis();
        Date startDate = new Date(now);
        Date endDate = new Date(now + TimeUnit.DAYS.toMillis(days));

        // A unique serial number for the certificate
        BigInteger serialNumber = new BigInteger(String.valueOf(now));

        // The subject and issuer are the same for a self-signed certificate
        X500Name subjectAndIssuer = new X500Name("CN=" + commonName);

        // 3. Create the Certificate Builder
        X509v3CertificateBuilder certificateBuilder = new X509v3CertificateBuilder(
                subjectAndIssuer,
                serialNumber,
                startDate,
                endDate,
                subjectAndIssuer,
                SubjectPublicKeyInfo.getInstance(publicKey.getEncoded())
        );

        // 4. Sign the certificate with the private key
        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSA")
                .setProvider("BC")
                .build(privateKey);

        X509Certificate certificate = new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certificateBuilder.build(contentSigner));

        // 5. Verify the certificate (optional but good practice)
        certificate.verify(publicKey);
        log.info("Certificate successfully generated and verified");

        // 6. Save Private Key to key.pem
        File keyFile = new File(certPath, KEY_FILE_NAME);
        saveAsPemFile(privateKey, keyFile);
        log.info("Private key saved to {}", keyFile.getAbsolutePath());

        // 7. Save Certificate to cert.pem
        File certFile = new File(certPath, CERT_FILE_NAME);
        saveAsPemFile(certificate, certFile);
        log.info("Certificate saved to {}", certFile.getAbsolutePath());
    }

    /**
     * Saves a cryptographic object (like a key or certificate) to a file in PEM format.
     *
     * @param object The object to save (e.g., PrivateKey, X509Certificate).
     * @param file   The file will be saved in PEM format.
     * @throws IOException if an I/O error occurs.
     */
    private void saveAsPemFile(Object object, File file) throws IOException {
        try (Writer writer = new FileWriter(file);
             JcaPEMWriter pemWriter = new JcaPEMWriter(writer)) {
            pemWriter.writeObject(object);
        }
    }
}
