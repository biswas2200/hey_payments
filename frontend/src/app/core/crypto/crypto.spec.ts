import { describe, expect, it } from 'vitest';
import * as fs from 'node:fs';
import * as path from 'node:path';
import { canonicalize } from './jcs';
import {
  generateDeviceKeyPair,
  exportPublicKeyJwk,
  exportPublicKeySec1,
  importPublicKeyJwk,
  importPublicKeySec1,
  signEnvelope,
  verifyEnvelope,
  sha256Hex,
} from './crypto';

interface JcsVector {
  name: string;
  input: unknown;
  expected: string;
}

interface SignVerifyVector {
  description: string;
  privateKeyPkcs8Base64: string;
  publicKeySec1Base64Url: string;
  publicKeyJwk: {
    kty: string;
    crv: string;
    kid: string;
    x: string;
    y: string;
  };
  payload: Record<string, unknown>;
  canonicalJson: string;
  signatureP1363Base64Url: string;
  tamperedPayload: Record<string, unknown>;
}

describe('Frontend Crypto & Shared Test Vectors', () => {
  const rootDir = path.resolve(__dirname, '../../../../../test-vectors');
  const jcsVectors: JcsVector[] = JSON.parse(
    fs.readFileSync(path.join(rootDir, 'jcs.json'), 'utf-8'),
  );
  const signVerifyVector: SignVerifyVector = JSON.parse(
    fs.readFileSync(path.join(rootDir, 'sign-verify.json'), 'utf-8'),
  );

  describe('RFC 8785 JSON Canonicalization Scheme (JCS)', () => {
    for (const vector of jcsVectors) {
      it(`should pass vector: ${vector.name}`, () => {
        const canonical = canonicalize(vector.input);
        expect(canonical).toBe(vector.expected);
      });
    }

    it('should match sign-verify vector canonicalJson exactly', () => {
      const canonical = canonicalize(signVerifyVector.payload);
      expect(canonical).toBe(signVerifyVector.canonicalJson);
    });

    it('should sort keys lexicographically by UTF-16 code units', () => {
      const input = { z: 1, a: 2, B: 3, aa: 4 };
      expect(canonicalize(input)).toBe('{"B":3,"a":2,"aa":4,"z":1}');
    });

    it('should handle nested arrays and primitives properly', () => {
      const input = { arr: [null, true, false, 0, -42, 3.1415], str: 'hello "world" \n' };
      expect(canonicalize(input)).toBe(
        '{"arr":[null,true,false,0,-42,3.1415],"str":"hello \\"world\\" \\n"}',
      );
    });
  });

  describe('WebCrypto P-256 Sign and Verify', () => {
    it('should generate non-extractable private key and extractable public key', async () => {
      const keyPair = await generateDeviceKeyPair();
      expect(keyPair.privateKey.extractable).toBe(false);
      expect(keyPair.publicKey.extractable).toBe(true);
      expect(keyPair.privateKey.algorithm.name).toBe('ECDSA');
    });

    it('should verify the shared cross-language sign-verify vector using imported JWK', async () => {
      const pubKey = await importPublicKeyJwk(signVerifyVector.publicKeyJwk);
      const envelope = {
        p: signVerifyVector.payload,
        s: signVerifyVector.signatureP1363Base64Url,
      };

      const valid = await verifyEnvelope(envelope, pubKey);
      expect(valid).toBe(true);
    });

    it('should verify the shared vector using imported SEC1 public key', async () => {
      const pubKey = await importPublicKeySec1(signVerifyVector.publicKeySec1Base64Url);
      const envelope = {
        p: signVerifyVector.payload,
        s: signVerifyVector.signatureP1363Base64Url,
      };

      const valid = await verifyEnvelope(envelope, pubKey);
      expect(valid).toBe(true);
    });

    it('should reject tampered payload against the vector signature', async () => {
      const pubKey = await importPublicKeyJwk(signVerifyVector.publicKeyJwk);
      const envelope = {
        p: signVerifyVector.tamperedPayload,
        s: signVerifyVector.signatureP1363Base64Url,
      };

      const valid = await verifyEnvelope(envelope, pubKey);
      expect(valid).toBe(false);
    });

    it('should export public key to JWK and uncompressed SEC1 matching specification', async () => {
      const keyPair = await generateDeviceKeyPair();
      const jwk = await exportPublicKeyJwk(keyPair.publicKey);
      expect(jwk.kty).toBe('EC');
      expect(jwk.crv).toBe('P-256');
      expect(jwk.x).toBeDefined();
      expect(jwk.y).toBeDefined();

      const sec1 = await exportPublicKeySec1(keyPair.publicKey);
      expect(sec1.length).toBe(65);
      expect(sec1[0]).toBe(0x04); // SEC1 uncompressed point prefix
    });

    it('should sign payload and verify envelope end-to-end with generated keys', async () => {
      const keyPair = await generateDeviceKeyPair();
      const payload = {
        v: 1,
        t: 'VC',
        voucherId: '01931234-0000-7000-8000-000000000099',
        amount: 25000,
        counter: 1,
      };

      const envelope = await signEnvelope(payload, keyPair.privateKey);
      expect(envelope.p).toEqual(payload);
      expect(envelope.s).toBeTruthy();

      const valid = await verifyEnvelope(envelope, keyPair.publicKey);
      expect(valid).toBe(true);

      // Verify tampered amount fails
      const tamperedEnvelope = {
        p: { ...payload, amount: 99999 },
        s: envelope.s,
      };
      const invalid = await verifyEnvelope(tamperedEnvelope, keyPair.publicKey);
      expect(invalid).toBe(false);
    });

    it('should compute SHA-256 hex correctly', async () => {
      const hash = await sha256Hex('hello');
      expect(hash).toBe('2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824');
    });
  });
});
