import { indexedDB, IDBKeyRange } from 'fake-indexeddb';

globalThis.indexedDB = indexedDB;
globalThis.IDBKeyRange = IDBKeyRange as unknown as typeof globalThis.IDBKeyRange;
