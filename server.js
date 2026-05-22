const express = require('express');
const admin = require('firebase-admin');
const cors = require('cors');
require('dotenv').config();

const serviceAccount = require('./serviceAccountKey.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();
const app = express();

app.use(cors());
app.use(express.json());

// ── GET all products ──────────────────────────────────────────
app.get('/api/products', async (req, res) => {
  try {
    const snapshot = await db.collection('products').get();
    const products = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    res.json(products);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// ── POST create order ─────────────────────────────────────────
app.post('/api/orders', async (req, res) => {
  try {
    const { items, total, userId } = req.body;
    const order = {
      items,
      total,
      userId: userId || 'anonymous',
      timestamp: Date.now()
    };
    const ref = await db.collection('orders').add(order);
    res.json({ success: true, orderId: ref.id });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// ── GET orders ────────────────────────────────────────────────
app.get('/api/orders', async (req, res) => {
  try {
    const snapshot = await db.collection('orders').get();
    const orders = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() }));
    res.json(orders);
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// ── GET wishlist ──────────────────────────────────────────────
app.get('/api/wishlist/:userId', async (req, res) => {
  try {
    const doc = await db.collection('wishlists').doc(req.params.userId).get();
    res.json(doc.exists ? doc.data() : { productIds: [] });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// ── POST update wishlist ──────────────────────────────────────
app.post('/api/wishlist/:userId', async (req, res) => {
  try {
    const { productIds } = req.body;
    await db.collection('wishlists').doc(req.params.userId).set({ productIds });
    res.json({ success: true });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// ── Health check ──────────────────────────────────────────────
app.get('/', (req, res) => {
  res.json({ message: 'Real Madrid Store Backend is running! Hala Madrid! 🏆' });
});

const PORT = 3000;
app.listen(PORT, '0.0.0.0', () => {
  console.log(`✅ Server running on http://0.0.0.0:${PORT}`);
});