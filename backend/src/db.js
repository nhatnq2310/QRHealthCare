import mongoose from "mongoose";

export async function connectDb(uri) {
  if (!uri) throw new Error("MONGODB_URI is missing — copy .env.example to .env");

  // Strict mode hides accidental typos; useful early in development.
  mongoose.set("strictQuery", true);

  await mongoose.connect(uri, { serverSelectionTimeoutMS: 5000 });
  console.log(`[db] connected to ${uri.replace(/:[^:@/]+@/, ":***@")}`);
}
