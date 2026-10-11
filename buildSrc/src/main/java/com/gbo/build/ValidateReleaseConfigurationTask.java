package com.gbo.build;

import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Arrays;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

/** Release guard with no references to a project, extension or Kotlin DSL script. */
@DisableCachingByDefault(because = "Release validation has no outputs and must execute on every invocation")
public abstract class ValidateReleaseConfigurationTask extends DefaultTask {
    @Input public abstract Property<Boolean> getValidationBuild();
    @Input public abstract Property<Boolean> getSigningConfigured();
    @Input public abstract Property<Boolean> getUserServiceConfigured();
    @Input public abstract Property<Boolean> getDriveConfigured();
    @Input public abstract Property<String> getExpectedCertificateSha256();

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    public abstract RegularFileProperty getKeystoreFile();

    // No outputs/up-to-date reuse: credentials need not become task input fingerprints.
    // @Internal does NOT exclude values from the encrypted Configuration Cache.
    @Internal public abstract Property<String> getStorePassword();
    @Internal public abstract Property<String> getKeyAlias();
    @Internal public abstract Property<String> getKeyPassword();

    @TaskAction
    public void validate() throws Exception {
        require(!getValidationBuild().get(),
                "PRODUCTION_BLOCKED: gboValidationBuild no es una build de campo.");
        require(getSigningConfigured().get(),
                "RELEASE_BLOCKED_MISSING_SIGNING_KEY: configura la keystore histórica y las cuatro propiedades GBO_RELEASE_* localmente. No hay fallback debug.");
        require(getUserServiceConfigured().get(),
                "PRODUCTION_BLOCKED: falta SERVER_BASE_URL autorizado en server.local.properties.");
        require(getDriveConfigured().get(),
                "PRODUCTION_BLOCKED: falta configuración Drive autorizada.");

        KeyStore keyStore;
        char[] storePassword = getStorePassword().get().toCharArray();
        try {
            keyStore = KeyStore.getInstance(getKeystoreFile().get().getAsFile(), storePassword);
        } catch (Exception ignored) {
            throw new GradleException("PRODUCTION_BLOCKED: no se pudo abrir la keystore histórica con la configuración local.");
        } finally {
            Arrays.fill(storePassword, '\0');
        }
        String alias = getKeyAlias().get();
        require(keyStore.isKeyEntry(alias),
                "PRODUCTION_BLOCKED: no hay clave privada para el alias configurado.");
        java.security.Key privateKey;
        char[] keyPassword = getKeyPassword().get().toCharArray();
        try {
            privateKey = keyStore.getKey(alias, keyPassword);
        } catch (Exception ignored) {
            throw new GradleException("PRODUCTION_BLOCKED: no se pudo abrir la clave privada con la configuración local.");
        } finally {
            Arrays.fill(keyPassword, '\0');
        }
        require(privateKey instanceof PrivateKey,
                "PRODUCTION_BLOCKED: el alias no contiene una clave privada de firma.");
        Certificate certificate = keyStore.getCertificate(alias);
        require(certificate != null, "PRODUCTION_BLOCKED: falta certificado de firma.");
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(certificate.getEncoded());
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            hex.append(Character.forDigit((value >>> 4) & 15, 16));
            hex.append(Character.forDigit(value & 15, 16));
        }
        require(hex.toString().equals(getExpectedCertificateSha256().get()),
                "SIGNATURE_MISMATCH_CONFIRMED: la keystore no coincide con el certificado histórico esperado.");
        require(!certificate.toString().contains("CN=Android Debug"),
                "PRODUCTION_BLOCKED: certificado debug.");
        getLogger().lifecycle("Release configuration and historical certificate verified; no secrets displayed.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new GradleException(message);
    }
}
