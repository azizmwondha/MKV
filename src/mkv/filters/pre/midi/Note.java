package mkv.filters.pre.midi;

/**
 * Created by benmusch on 12/10/15.
 */
public class Note {
  private int start, end, instrument, volume, pitch;

  public Note(int start, int instrument, int volume, int pitch) {
    this.start = start;
    this.instrument = instrument;
    this.volume = volume;
    this.pitch = pitch;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof Note) {
      Note n = (Note) o;
      return n.pitch == pitch && n.instrument == instrument;
    }
    else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return instrument * 3 + pitch * 7;
  }

  public void finish(int end) {
    this.end = end;
  }

  @Override
  public String toString() {
    return "note " + start + " " + end + " " + instrument + " " + pitch + " " + volume;
  }
}