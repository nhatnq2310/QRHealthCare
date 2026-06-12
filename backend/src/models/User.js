import mongoose from "mongoose";

/**
 * Mongoose stores documents with `_id` (ObjectId). MockAPI returned `id`
 * as a string. The Android client expects `id`, so every schema in this
 * project applies the same `toJSON` transform: surface `id`, hide `_id`
 * and the version key `__v`.
 */
export const idTransform = {
  virtuals: true,
  versionKey: false,
  transform(_doc, ret) {
    ret.id = ret._id.toString();
    delete ret._id;
    return ret;
  },
};

const userSchema = new mongoose.Schema(
  {
    email:        { type: String, required: true, unique: true, lowercase: true, trim: true },
    passwordHash: { type: String, required: true }, // bcrypt — never sent to client
    fullName:     { type: String, default: "" },
    address:      { type: String, default: "" }, // user's shipping address
    role:         { type: String, enum: ["user", "admin"], default: "user" },
    createdAt:    { type: Number, default: () => Date.now() }, // epoch ms, matches Kotlin Long
  },
  {
    toJSON: {
      ...idTransform,
      transform(doc, ret) {
        idTransform.transform(doc, ret);
        delete ret.passwordHash; // critical: never leak hashes
        return ret;
      },
    },
  }
);

export default mongoose.model("User", userSchema);
