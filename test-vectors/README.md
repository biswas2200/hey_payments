# Test Vectors (HP-OFFLINE v1)

Cross-platform test vectors shared between backend (Java 21) and frontend (TypeScript 6).
These vectors ensure byte-for-byte protocol interoperability, canonical JSON encoding (RFC 8785 JCS), and cryptographic signature validation (ECDSA P-256 with SHA-256, IEEE P1363).

## Directory Structure
- `keys/`: Shared test public/private keypairs (server, merchant device, payer device)
- `certificates/`: Balance Certificate (BC) and Merchant Certificate (MC) vectors
- `vouchers/`: Payment Request (PR), Voucher (VC), and Receipt (RC) vectors
- `envelopes/`: Base45 and DEFLATE encoded wire format vectors

## Protocol Rules
- All amounts in minor units (paise).
- Never hand-edit vector JSON files without an approved protocol proposal in `docs/proposals/`.
- If one implementation fails a vector, the bug is in the implementation, not the vector.
