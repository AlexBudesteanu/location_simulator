package com.laird.geotracker.controller;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.ResourceBundle;

import com.lynden.gmapsfx.GoogleMapView;
import com.lynden.gmapsfx.MapComponentInitializedListener;
import com.lynden.gmapsfx.javascript.object.GoogleMap;
import com.lynden.gmapsfx.javascript.object.LatLong;
import com.lynden.gmapsfx.javascript.object.MapOptions;
import com.lynden.gmapsfx.javascript.object.MapTypeIdEnum;
import com.lynden.gmapsfx.javascript.object.Marker;
import com.lynden.gmapsfx.javascript.object.MarkerOptions;

import io.netty.buffer.ByteBuf;
import io.netty.channel.socket.DatagramPacket;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.channel.ConnectionHandler;
import io.reactivex.netty.channel.ObservableConnection;
import io.reactivex.netty.protocol.udp.server.UdpServer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.util.Pair;
import rx.Observable;
import rx.functions.Func1;

class RxUDPSever {

	private int port;
	private GoogleMap map;
	private Marker currentPos;
	private boolean isClosed;
	private SimpleDateFormat dateFormatter;

	private UdpServer<DatagramPacket, DatagramPacket> UDPServer;
	
	public RxUDPSever(int port, GoogleMap map) {
		this.port = port;
		this.map = map;
		this.currentPos = new Marker(new MarkerOptions().visible(false));
		this.map.addMarker(currentPos);
		this.isClosed = true;
		this.dateFormatter = new SimpleDateFormat("dd-MM-yyyy");
	}
	
	public Marker getCurrentPositionMarker() {
		return this.currentPos;
	}
	
	public void startUdpServer(){
		if(this.isClosed) {
			UDPServer = createServer();
			UDPServer.start();
			currentPos.setVisible(true);
			this.isClosed = false;
		} else {
			System.out.println("Server already started.");
		}
	}
	
	public void closeUdpServer() throws InterruptedException {
		if(!this.isClosed) {
			UDPServer.shutdown();
			currentPos.setVisible(false);
			currentPos.setPosition(null);
			this.isClosed = true;
		} else {
			System.out.println("Server already closed.");
		}
	}
	
	private Pair<Double,Double> getPosition(ByteBuf buffer) {
		int index = buffer.readerIndex();
		for(int i=index;i<16;i++) {
			System.out.print(String.format("%02X ",buffer.getByte(index)));
		}
		double lon = buffer.getDouble(index);
		index += Double.BYTES;
		double lat = buffer.getDouble(index);
		return new Pair<Double, Double>(lat, lon);
	}
	
	private UdpServer<DatagramPacket, DatagramPacket> createServer() {
		UdpServer<DatagramPacket, DatagramPacket> server = RxNetty.createUdpServer(port,
				new ConnectionHandler<DatagramPacket, DatagramPacket>() {

					@Override
					public Observable<Void> handle(ObservableConnection<DatagramPacket, DatagramPacket> newConnection) {

						return newConnection.getInput().flatMap(new Func1<DatagramPacket, Observable<Void>>() {

							@Override
							public Observable<Void> call(DatagramPacket received) {
								System.out.println(String.format("from: %s", received.sender().getAddress().getHostAddress()));
								System.out.println(dateFormatter.format(Calendar.getInstance().getTime()));
								ByteBuf buffer = received.content();
								Pair<Double,Double> pos = getPosition(buffer);
								System.out.println(String.format("%f, %f", pos.getKey(), pos.getValue()));			
								Platform.runLater(() ->{
									LatLong position = new LatLong(pos.getKey(), pos.getValue());
									currentPos.setPosition(position);
								});
								return Observable.just(null);
							}
						});
					}
				});

		return server;
	}
}

public class MapController implements Initializable, MapComponentInitializedListener {

	@FXML
	protected GoogleMapView mapView;

	@FXML
	protected Button startButton;

	@FXML
	protected Button stopButton;
	
	RxUDPSever rxServer;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		mapView.addMapInializedListener(this);
		
		startButton.setOnMouseClicked(event -> {
			rxServer.startUdpServer();				
		});
		
		stopButton.setOnMouseClicked(event -> {
			try {
				rxServer.closeUdpServer();
			}  catch (InterruptedException e) {
				e.printStackTrace();
			}
		});
	}

	@Override
	public void mapInitialized() {
		MapOptions options = new MapOptions();

		options.center(new LatLong(44.4267674,26.1025384)).zoomControl(true).zoom(10).mapType(MapTypeIdEnum.ROADMAP);
		GoogleMap map = mapView.createMap(options);
		rxServer = new RxUDPSever(8888, map);
	}

}
