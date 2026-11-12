package com.heypayments.crypto.domain;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoService {

  private static final String EC_ALGORITHM = "EC";
  private static final String P256_CURVE = "secp256r1";
  private static final String SIGNATURE_ALGORITHM = "SHA256withECDSAinP1363Format";
  private static final String AES_GCM_ALGORITHM = "AES/GCM/NoPadding";
  private static final int GCM_IV_LENGTH_BYTES = 12;
  private static final int GCM_TAG_LENGTH_BITS = 128;
  private static final int COORD_BYTE_LENGTH = 32;

  private final JcsCanonicalizer jcs;
  private final SecureRandom secureRandom;
  private final ECParameterSpec ecParameterSpec;

  public CryptoService(JcsCanonicalizer jcs) {
    this.jcs = jcs;
    this.secureRandom = new SecureRandom();
    try {
      AlgorithmParameters params = AlgorithmParameters.getInstance(EC_ALGORITHM);
      params.init(new ECGenParameterSpec(P256_CURVE));
      this.ecParameterSpec = params.getParameterSpec(ECParameterSpec.class);
    } catch (Exception e) {
      throw new CryptoException("Failed to initialize EC curve parameter spec", e);
    }
  }

  public KeyPair generateP256KeyPair() {
    try {
      KeyPairGenerator kpg = KeyPairGenerator.getInstance(EC_ALGORITHM);
      kpg.initialize(new ECGenParameterSpec(P256_CURVE), secureRandom);
      return kpg.generateKeyPair();
    } catch (Exception e) {
      throw new CryptoException("Failed to generate P-256 keypair", e);
    }
  }

  public byte[] generateMasterKey() {
    byte[] key = new byte[32];
    secureRandom.nextBytes(key);
    return key;
  }

  public byte[] toUncompressedSec1(ECPublicKey publicKey) {
    byte[] x = toFixedLengthByteArray(publicKey.getW().getAffineX(), COORD_BYTE_LENGTH);
    byte[] y = toFixedLengthByteArray(publicKey.getW().getAffineY(), COORD_BYTE_LENGTH);

    byte[] sec1 = new byte[1 + 2 * COORD_BYTE_LENGTH];
    sec1[0] = 0x04; // uncompressed indicator
    System.arraycopy(x, 0, sec1, 1, COORD_BYTE_LENGTH);
    System.arraycopy(y, 0, sec1, 1 + COORD_BYTE_LENGTH, COORD_BYTE_LENGTH);
    return sec1;
  }

  public ECPublicKey fromUncompressedSec1(byte[] sec1) {
    if (sec1 == null || sec1.length != 65 || sec1[0] != 0x04) {
      throw new CryptoException("Invalid uncompressed SEC1 point: expected 65 bytes starting with 0x04");
    }
    byte[] xBytes = Arrays.copyOfRange(sec1, 1, 33);
    byte[] yBytes = Arrays.copyOfRange(sec1, 33, 65);

    BigInteger x = new BigInteger(1, xBytes);
    BigInteger y = new BigInteger(1, yBytes);
    ECPoint w = new ECPoint(x, y);

    try {
      KeyFactory kf = KeyFactory.getInstance(EC_ALGORITHM);
      return (ECPublicKey) kf.generatePublic(new ECPublicKeySpec(w, ecParameterSpec));
    } catch (Exception e) {
      throw new CryptoException("Failed to parse EC public key from SEC1 bytes", e);
    }
  }

  public Map<String, String> toJwk(ECPublicKey publicKey, String kid) {
    byte[] x = toFixedLengthByteArray(publicKey.getW().getAffineX(), COORD_BYTE_LENGTH);
    byte[] y = toFixedLengthByteArray(publicKey.getW().getAffineY(), COORD_BYTE_LENGTH);

    Map<String, String> jwk = new LinkedHashMap<>();
    jwk.put("kty", "EC");
    jwk.put("crv", "P-256");
    if (kid != null) {
      jwk.put("kid", kid);
    }
    jwk.put("x", Base64.getUrlEncoder().withoutPadding().encodeToString(x));
    jwk.put("y", Base64.getUrlEncoder().withoutPadding().encodeToString(y));
    return jwk;
  }

  public ECPublicKey fromJwk(String xBase64Url, String yBase64Url) {
    byte[] xBytes = Base64.getUrlDecoder().decode(xBase64Url);
    byte[] yBytes = Base64.getUrlDecoder().decode(yBase64Url);

    BigInteger x = new BigInteger(1, xBytes);
    BigInteger y = new BigInteger(1, yBytes);
    ECPoint w = new ECPoint(x, y);

    try {
      KeyFactory kf = KeyFactory.getInstance(EC_ALGORITHM);
      return (ECPublicKey) kf.generatePublic(new ECPublicKeySpec(w, ecParameterSpec));
    } catch (Exception e) {
      throw new CryptoException("Failed to construct EC public key from JWK coordinates", e);
    }
  }

  public ECPrivateKey privateKeyFromEncoded(byte[] pkcs8Bytes) {
    try {
      KeyFactory kf = KeyFactory.getInstance(EC_ALGORITHM);
      return (ECPrivateKey) kf.generatePrivate(new PKCS8EncodedKeySpec(pkcs8Bytes));
    } catch (Exception e) {
      throw new CryptoException("Failed to load EC private key from PKCS8 bytes", e);
    }
  }

  public SignedEnvelope signEnvelope(Map<String, Object> payload, ECPrivateKey privateKey) {
    try {
      byte[] canonicalBytes = jcs.canonicalizeToBytes(payload);
      Signature signer = Signature.getInstance(SIGNATURE_ALGORITHM);
      signer.initSign(privateKey);
      signer.update(canonicalBytes);
      byte[] signatureBytes = signer.sign();

      String signatureBase64Url = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
      return new SignedEnvelope(payload, signatureBase64Url);
    } catch (Exception e) {
      throw new CryptoException("Failed to sign envelope payload", e);
    }
  }

  public boolean verifyEnvelope(SignedEnvelope envelope, ECPublicKey publicKey) {
    try {
      byte[] canonicalBytes = jcs.canonicalizeToBytes(envelope.p());
      byte[] signatureBytes = Base64.getUrlDecoder().decode(envelope.s());

      Signature verifier = Signature.getInstance(SIGNATURE_ALGORITHM);
      verifier.initVerify(publicKey);
      verifier.update(canonicalBytes);
      return verifier.verify(signatureBytes);
    } catch (Exception e) {
      return false;
    }
  }

  public byte[] encryptPrivateKey(byte[] privateKeyDer, byte[] masterKey) {
    try {
      byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
      secureRandom.nextBytes(iv);

      Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
      SecretKey key = new SecretKeySpec(masterKey, "AES");
      GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
      cipher.init(Cipher.ENCRYPT_MODE, key, spec);

      byte[] cipherText = cipher.doFinal(privateKeyDer);

      byte[] result = new byte[iv.length + cipherText.length];
      System.arraycopy(iv, 0, result, 0, iv.length);
      System.arraycopy(cipherText, 0, result, iv.length, cipherText.length);
      return result;
    } catch (Exception e) {
      throw new CryptoException("Failed to encrypt private key with AES-256-GCM", e);
    }
  }

  public byte[] decryptPrivateKey(byte[] cipherTextWithIv, byte[] masterKey) {
    try {
      if (cipherTextWithIv == null || cipherTextWithIv.length < GCM_IV_LENGTH_BYTES) {
        throw new CryptoException("Ciphertext payload too short for AES-GCM IV");
      }
      byte[] iv = Arrays.copyOfRange(cipherTextWithIv, 0, GCM_IV_LENGTH_BYTES);
      byte[] cipherText = Arrays.copyOfRange(cipherTextWithIv, GCM_IV_LENGTH_BYTES, cipherTextWithIv.length);

      Cipher cipher = Cipher.getInstance(AES_GCM_ALGORITHM);
      SecretKey key = new SecretKeySpec(masterKey, "AES");
      GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
      cipher.init(Cipher.DECRYPT_MODE, key, spec);

      return cipher.doFinal(cipherText);
    } catch (Exception e) {
      throw new CryptoException("Failed to decrypt private key with AES-256-GCM", e);
    }
  }

  private byte[] toFixedLengthByteArray(BigInteger bi, int length) {
    byte[] src = bi.toByteArray();
    if (src.length == length) {
      return src;
    }
    byte[] dest = new byte[length];
    if (src.length > length) {
      // Drop leading sign byte
      System.arraycopy(src, src.length - length, dest, 0, length);
    } else {
      // Pad with leading zeros
      System.arraycopy(src, 0, dest, length - src.length, src.length);
    }
    return dest;
  }
}
