package main;

public class PWSParameters {
	private int humidityThreshold = -1;
	private long wateringTime = -1;
	private long resumeWatchAfter = -1;

	public PWSParameters() {}
	public PWSParameters humidityThreshold(int ht) {
		this.humidityThreshold = ht;
		return this;
	}
	public PWSParameters wateringTime(long wt) {
		this.wateringTime = wt;
		return this;
	}
	public PWSParameters resumeWatchAfter(long rwa) {
		this.resumeWatchAfter = rwa;
		return this;
	}
	public int humidityThreshold() {
		return this.humidityThreshold;
	}
	public long wateringTime() {
		return this.wateringTime;
	}
	public long resumeWatchAfter() {
		return this.resumeWatchAfter;
	}
}
