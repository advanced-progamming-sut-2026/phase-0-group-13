package view.gdx.animation;

import com.badlogic.gdx.files.FileHandle;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class PamFile {

  private static final long MAGIC = 0xBAF01954L;
  private static final int MAX_VERSION = 6;

  final float frameRate;
  final float[] canvas;
  final List<Image> images;
  final List<Sprite> sprites;
  final Sprite mainSprite;

  private PamFile(float frameRate, float[] canvas, List<Image> images, List<Sprite> sprites,
      Sprite mainSprite) {
    this.frameRate = frameRate;
    this.canvas = canvas;
    this.images = images;
    this.sprites = sprites;
    this.mainSprite = mainSprite;
  }

  static PamFile read(FileHandle file) {
    if (file == null || !file.exists()) {
      return null;
    }
    try {
      return new Reader(file.readBytes()).read();
    } catch (RuntimeException e) {
      return null;
    }
  }

  static final class Image {
    final String leaf;
    final int[] size;
    final float[] transform;

    Image(String leaf, int[] size, float[] transform) {
      this.leaf = leaf;
      this.size = size;
      this.transform = transform;
    }
  }

  static final class Sprite {
    final String name;
    final float frameRate;
    final List<Frame> frames;

    Sprite(String name, float frameRate, List<Frame> frames) {
      this.name = name;
      this.frameRate = frameRate;
      this.frames = frames;
    }
  }

  static final class Frame {
    final String clip;
    final boolean stop;
    final List<Integer> removes;
    final List<Add> appends;
    final List<Move> changes;

    Frame(String clip, boolean stop, List<Integer> removes, List<Add> appends, List<Move> changes) {
      this.clip = clip;
      this.stop = stop;
      this.removes = removes;
      this.appends = appends;
      this.changes = changes;
    }
  }

  static final class Add {
    final int index;
    final String name;
    final int resource;
    final boolean sprite;
    final float timescale;

    Add(int index, String name, int resource, boolean sprite, float timescale) {
      this.index = index;
      this.name = name;
      this.resource = resource;
      this.sprite = sprite;
      this.timescale = timescale;
    }
  }

  static final class Move {
    final int index;
    final float[] transform;
    final float[] color;
    final int animFrameNum;

    Move(int index, float[] transform, float[] color, int animFrameNum) {
      this.index = index;
      this.transform = transform;
      this.color = color;
      this.animFrameNum = animFrameNum;
    }
  }

  private static final class Reader {

    private static final int M_SRC_RECT = 32768;
    private static final int M_ROTATE = 16384;
    private static final int M_COLOR = 8192;
    private static final int M_MATRIX = 4096;
    private static final int M_LONG_COORDS = 2048;
    private static final int M_ANIM_FRAME = 1024;

    private final byte[] bytes;
    private int at;

    Reader(byte[] bytes) {
      this.bytes = bytes;
    }

    PamFile read() {
      if (u32() != MAGIC) {
        throw new IllegalArgumentException("not a PAM");
      }
      int version = i32();
      if (version > MAX_VERSION) {
        throw new IllegalArgumentException("unsupported PAM version " + version);
      }
      float frameRate = u8();
      i16();
      i16();
      float[] canvas = {i16() / 20f, i16() / 20f};

      int imageCount = i16();
      List<Image> images = new ArrayList<>(Math.max(0, imageCount));
      for (int i = 0; i < imageCount; i++) {
        images.add(image(version));
      }
      int spriteCount = i16();
      List<Sprite> sprites = new ArrayList<>(Math.max(0, spriteCount));
      for (int i = 0; i < spriteCount; i++) {
        sprites.add(sprite(version, frameRate));
      }
      Sprite main = version <= 3 || u8() != 0 ? sprite(version, frameRate) : null;
      return new PamFile(frameRate, canvas, images, sprites, main);
    }

    private Image image(int version) {
      String name = string();
      int[] size = {-1, -1};
      if (version >= 4) {
        size[0] = i16();
        size[1] = i16();
      }
      float[] transform = new float[6];
      if (version == 1) {
        double radians = i16() / 1000d;
        transform[0] = (float) Math.cos(radians);
        transform[2] = (float) -Math.sin(radians);
        transform[1] = (float) Math.sin(radians);
        transform[3] = (float) Math.cos(radians);
      } else {
        transform[0] = (float) (i32() / 1310720d);
        transform[2] = (float) (i32() / 1310720d);
        transform[1] = (float) (i32() / 1310720d);
        transform[3] = (float) (i32() / 1310720d);
      }
      transform[4] = i16() / 20f;
      transform[5] = i16() / 20f;
      int pipe = name.indexOf('|');
      return new Image(pipe >= 0 ? name.substring(0, pipe) : name, size, transform);
    }

    private Sprite sprite(int version, float globalFrameRate) {
      String name = null;
      float frameRate = globalFrameRate;
      if (version >= 4) {
        name = string();
        if (version >= 6) {
          string();
        }
        frameRate = (float) (i32() / 65536d);
      }
      int frameCount = i16();
      if (version >= 5) {
        i16();
        i16();
      }
      List<Frame> frames = new ArrayList<>(Math.max(0, frameCount));
      for (int i = 0; i < frameCount; i++) {
        frames.add(frame(version));
      }
      return new Sprite(name, frameRate, frames);
    }

    private Frame frame(int version) {
      int flags = u8();
      List<Integer> removes = new ArrayList<>();
      if ((flags & 0x01) != 0) {
        for (int i = 0, n = count(); i < n; i++) {
          int index = i16();
          removes.add(index >= 2047 ? i32() : index);
        }
      }
      List<Add> appends = new ArrayList<>();
      if ((flags & 0x02) != 0) {
        for (int i = 0, n = count(); i < n; i++) {
          appends.add(add(version));
        }
      }
      List<Move> changes = new ArrayList<>();
      if ((flags & 0x04) != 0) {
        for (int i = 0, n = count(); i < n; i++) {
          changes.add(move());
        }
      }
      String clip = (flags & 0x08) != 0 ? string() : null;
      boolean stop = (flags & 0x10) != 0;
      if ((flags & 0x20) != 0) {
        for (int i = 0, n = u8(); i < n; i++) {
          string();
          string();
        }
      }
      return new Frame(clip, stop, removes, appends, changes);
    }

    private Add add(int version) {
      int flags = u16();
      int index = flags & 2047;
      if (index == 2047) {
        index = i32();
      }
      boolean sprite = (flags & 32768) != 0;
      int resource = u8();
      if (version >= 6 && resource == 255) {
        resource = i16();
      }
      if ((flags & 8192) != 0) {
        i16();
      }
      String name = (flags & 4096) != 0 ? string() : null;
      float timescale = (flags & 2048) != 0 ? i32() / 65536f : 1f;
      return new Add(index, name, resource, sprite, timescale);
    }

    private Move move() {
      int flags = u16();
      int index = flags & 1023;
      if (index == 1023) {
        index = i32();
      }
      float[] transform;
      if ((flags & M_MATRIX) != 0) {
        transform = new float[6];
        transform[0] = (float) (i32() / 65536d);
        transform[2] = (float) (i32() / 65536d);
        transform[1] = (float) (i32() / 65536d);
        transform[3] = (float) (i32() / 65536d);
      } else if ((flags & M_ROTATE) != 0) {
        transform = new float[3];
        transform[0] = i16() / 1000f;
      } else {
        transform = new float[2];
      }
      if ((flags & M_LONG_COORDS) != 0) {
        transform[transform.length - 2] = i32() / 20f;
        transform[transform.length - 1] = i32() / 20f;
      } else {
        transform[transform.length - 2] = i16() / 20f;
        transform[transform.length - 1] = i16() / 20f;
      }
      if ((flags & M_SRC_RECT) != 0) {
        i16();
        i16();
        i16();
        i16();
      }
      float[] color = null;
      if ((flags & M_COLOR) != 0) {
        color = new float[] {u8() / 255f, u8() / 255f, u8() / 255f, u8() / 255f};
      }
      int animFrameNum = (flags & M_ANIM_FRAME) != 0 ? i16() : 0;
      return new Move(index, transform, color, animFrameNum);
    }

    private int count() {
      int n = u8();
      return n == 255 ? i16() : n;
    }

    private int u8() {
      return bytes[at++] & 0xFF;
    }

    private int u16() {
      int value = (bytes[at] & 0xFF) | ((bytes[at + 1] & 0xFF) << 8);
      at += 2;
      return value;
    }

    private short i16() {
      return (short) u16();
    }

    private int i32() {
      int value = (bytes[at] & 0xFF) | ((bytes[at + 1] & 0xFF) << 8)
          | ((bytes[at + 2] & 0xFF) << 16) | ((bytes[at + 3] & 0xFF) << 24);
      at += 4;
      return value;
    }

    private long u32() {
      return i32() & 0xFFFFFFFFL;
    }

    private String string() {
      int length = u16();
      String value = new String(bytes, at, length, StandardCharsets.UTF_8);
      at += length;
      return value;
    }
  }
}
