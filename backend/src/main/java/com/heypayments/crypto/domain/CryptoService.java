package com.heypayments.crypto.domain;

import java.math.BigDecimal;
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

  private static final BigInteger P256_P =
      new BigInteger("FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFF", 16);
  private static final BigInteger P256_A =
      new BigInteger("FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFC", 16);
  private static final BigInteger P256_B =
      new BigInteger("5AC635D8AA3A93E7B3EBBD55769886BC651D06B0CC53B0F63BCE3C3E27D2604B", 16);

  public static final long MAX_SAFE_INTEGER = 9_007_199_254_740_991L; // 2^53 - 1
  public static final long MIN_SAFE_INTEGER = -9_007_199_254_740_991L;

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
      throw new CryptoException(
          "Invalid uncompressed SEC1 point: expected 65 bytes starting with 0x04");
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
    if (xBase64Url == null || xBase64Url.isBlank() || yBase64Url == null || yBase64Url.isBlank()) {
      throw new CryptoException("JWK x and y coordinates are missing or blank");
    }

    byte[] xBytes;
    byte[] yBytes;
    try {
      xBytes = Base64.getUrlDecoder().decode(xBase64Url);
      yBytes = Base64.getUrlDecoder().decode(yBase64Url);
    } catch (IllegalArgumentException e) {
      throw new CryptoException("Invalid base64url encoding for JWK coordinates", e);
    }

    if (xBytes.length != COORD_BYTE_LENGTH || yBytes.length != COORD_BYTE_LENGTH) {
      throw new CryptoException(
          "Invalid JWK coordinate lengths: expected 32 bytes each, got x="
              + xBytes.length
              + " y="
              + yBytes.length);
    }

    BigInteger x = new BigInteger(1, xBytes);
    BigInteger y = new BigInteger(1, yBytes);

    // Range check: 0 <= x < p, 0 <= y < p
    if (x.compareTo(BigInteger.ZERO) < 0
        || x.compareTo(P256_P) >= 0
        || y.compareTo(BigInteger.ZERO) < 0
        || y.compareTo(P256_P) >= 0) {
      throw new CryptoException("JWK coordinates out of field range [0, p-1]");
    }

    // Curve check: y^2 = x^3 + a*x + b (mod p)
    BigInteger lhs = y.modPow(BigInteger.valueOf(2), P256_P);
    BigInteger rhs =
        x.modPow(BigInteger.valueOf(3), P256_P).add(P256_A.multiply(x)).add(P256_B).mod(P256_P);

    if (!lhs.equals(rhs)) {
      throw new CryptoException("Point (x, y) is not on curve P-256");
    }

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

  public void validatePayloadNumbers(Object obj) {
    if (obj instanceof Map<?, ?> map) {
      for (Object val : map.values()) {
        validatePayloadNumbers(val);
      }
    } else if (obj instanceof Iterable<?> iter) {
      for (Object val : iter) {
        validatePayloadNumbers(val);
      }
    } else if (obj instanceof Object[] arr) {
      for (Object val : arr) {
        validatePayloadNumbers(val);
      }
    } else if (obj instanceof Number num) {
      if (num instanceof Double d) {
        if (!Double.isFinite(d) || Math.floor(d) != d) {
          throw new CryptoException("Protocol payload rejected: non-integer number: " + d);
        }
        if (d > MAX_SAFE_INTEGER || d < MIN_SAFE_INTEGER) {
          throw new CryptoException("Protocol payload rejected: integer exceeds 2^53 - 1: " + d);
        }
      } else if (num instanceof Float f) {
        if (!Float.isFinite(f) || Math.floor(f) != f) {
          throw new CryptoException("Protocol payload rejected: non-integer number: " + f);
        }
        if (f > MAX_SAFE_INTEGER || f < MIN_SAFE_INTEGER) {
          throw new CryptoException("Protocol payload rejected: integer exceeds 2^53 - 1: " + f);
        }
      } else if (num instanceof BigDecimal bd) {
        if (bd.scale() > 0 && bd.stripTrailingZeros().scale() > 0) {
          throw new CryptoException("Protocol payload rejected: non-integer number: " + bd);
        }
        if (bd.compareTo(BigDecimal.valueOf(MAX_SAFE_INTEGER)) > 0
            || bd.compareTo(BigDecimal.valueOf(MIN_SAFE_INTEGER)) < 0) {
          throw new CryptoException("Protocol payload rejected: integer exceeds 2^53 - 1: " + bd);
        }
      } else if (num instanceof BigInteger bi) {
        if (bi.compareTo(BigInteger.valueOf(MAX_SAFE_INTEGER)) > 0
            || bi.compareTo(BigInteger.valueOf(MIN_SAFE_INTEGER)) < 0) {
          throw new CryptoException("Protocol payload rejected: integer exceeds 2^53 - 1: " + bi);
        }
      } else {
        long val = num.longValue();
        if (val > MAX_SAFE_INTEGER || val < MIN_SAFE_INTEGER) {
          throw new CryptoException("Protocol payload rejected: integer exceeds 2^53 - 1: " + val);
        }
      }
    }
  }

  public SignedEnvelope signEnvelope(Map<String, Object> payload, ECPrivateKey privateKey) {
    try {
      if (payload == null) {
        throw new CryptoException("Payload cannot be null");
      }
      validatePayloadNumbers(payload);
      byte[] canonicalBytes = jcs.canonicalizeToBytes(payload);
      Signature signer = Signature.getInstance(SIGNATURE_ALGORITHM);
      signer.initSign(privateKey);
      signer.update(canonicalBytes);
      byte[] signatureBytes = signer.sign();

      String signatureBase64Url =
          Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
      return new SignedEnvelope(payload, signatureBase64Url);
    } catch (CryptoException e) {
      throw e;
    } catch (Exception e) {
      throw new CryptoException("Failed to sign envelope payload", e);
    }
  }

  public boolean verifyEnvelope(SignedEnvelope envelope, ECPublicKey publicKey) {
    try {
      if (envelope == null || envelope.p() == null || envelope.s() == null) {
        return false;
      }
      validatePayloadNumbers(envelope.p());
      byte[] signatureBytes = Base64.getUrlDecoder().decode(envelope.s());
      if (signatureBytes.length != 64) {
        return false;
      }

      byte[] canonicalBytes = jcs.canonicalizeToBytes(envelope.p());

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
      byte[] cipherText =
          Arrays.copyOfRange(cipherTextWithIv, GCM_IV_LENGTH_BYTES, cipherTextWithIv.length);

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
