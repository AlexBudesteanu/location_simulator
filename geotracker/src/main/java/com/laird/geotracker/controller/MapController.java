package com.laird.geotracker.controller;

import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.ResourceBundle;

import com.lynden.gmapsfx.GoogleMapView;
import com.lynden.gmapsfx.MapComponentInitializedListener;
import com.lynden.gmapsfx.javascript.event.UIEventHandler;
import com.lynden.gmapsfx.javascript.event.UIEventType;
import com.lynden.gmapsfx.javascript.object.GoogleMap;
import com.lynden.gmapsfx.javascript.object.InfoWindow;
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
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.util.Pair;
import netscape.javascript.JSObject;
import rx.Observable;
import rx.functions.Func1;

class RxUDPSever {

	private int port;
	private GoogleMap map;
	private Marker startPos;
	private boolean isClosed;
	private SimpleDateFormat dateFormatter;
	private MarkerOptions defaultMarkerOptions;

	private UdpServer<DatagramPacket, DatagramPacket> UDPServer;
	
	public RxUDPSever(int port, GoogleMap map) {
		this.port = port;
		this.map = map;
		this.startPos = null;
		this.isClosed = true;
		this.dateFormatter = new SimpleDateFormat("dd-MM-yyyy - HH:mm:ss");
		this.defaultMarkerOptions = new MarkerOptions().visible(true);
	}
	
	public void startUdpServer(){
		if(this.isClosed) {
			UDPServer = createServer();
			UDPServer.start();
			this.isClosed = false;
		} else {
			alert("Server already started.");
		}
	}
	
	public void closeUdpServer() throws InterruptedException {
		if(!this.isClosed) {
			UDPServer.shutdown();
			this.isClosed = true;
		} else {
			alert("Server already closed.");
		}
	}
	
	private Pair<Double,Double> getPosition(ByteBuf buffer) {
		ByteBuffer leBuffer = buffer.nioBuffer();
		leBuffer.order(ByteOrder.LITTLE_ENDIAN);
		double lon = leBuffer.getDouble();
		double lat = leBuffer.getDouble();
		return new Pair<Double, Double>(lat, lon);
	}
	
	private void alert(String message){
		Alert alert = new Alert(AlertType.WARNING);
		alert.setTitle("Warning!");
		alert.setContentText(message);
		alert.showAndWait();
	}
	
	private void appendMarkersWithCallback(Marker marker){
		ArrayList<Marker> temp = new ArrayList<Marker>();
		temp.add(marker);
		map.addMarkers(temp, UIEventType.click, (Marker m) -> {
			return new UIEventHandler() {
				
				@Override
				public void handle(JSObject arg0) {
					InfoWindow window = new InfoWindow();
					window.setContent(marker.getTitle());
					window.open(map, m);
				}
			};
		});
	}
	
	private UdpServer<DatagramPacket, DatagramPacket> createServer() {
		UdpServer<DatagramPacket, DatagramPacket> server = RxNetty.createUdpServer(port,
				new ConnectionHandler<DatagramPacket, DatagramPacket>() {

					@Override
					public Observable<Void> handle(ObservableConnection<DatagramPacket, DatagramPacket> newConnection) {

						return newConnection.getInput().flatMap(new Func1<DatagramPacket, Observable<Void>>() {

							@Override
							public Observable<Void> call(DatagramPacket received) {
								final String timestamp = dateFormatter.format(Calendar.getInstance().getTime());
								System.out.println(String.format("from: %s at %s",
										received.sender().getAddress().getHostAddress(),timestamp));
								ByteBuf buffer = received.content();
								Pair<Double,Double> pos = getPosition(buffer);
								System.out.println(String.format("lat: %f\nlng: %f", pos.getKey(), pos.getValue()));		
								Platform.runLater(() ->{
									final LatLong position = new LatLong(pos.getKey(), pos.getValue());
									String markerTitle = String.format("%s\n%s,%s", timestamp,position.getLatitude(),
											position.getLongitude());
									if(startPos == null){
										startPos = new Marker(defaultMarkerOptions);
										startPos.setPosition(position);
										startPos.setTitle(markerTitle);
										appendMarkersWithCallback(startPos);
									} else {
										Marker newPosition = new Marker(defaultMarkerOptions);
										newPosition.setPosition(position);
										newPosition.setTitle(markerTitle);
										appendMarkersWithCallback(newPosition);
									}
									map.setCenter(position);
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
		options.center(new LatLong(44.4267674,26.1025384)).overviewMapControl(false)
		.panControl(false).rotateControl(false).scaleControl(false).streetViewControl(false)
		.zoomControl(false).zoom(11).mapType(MapTypeIdEnum.SATELLITE);
		GoogleMap map = mapView.createMap(options);
		rxServer = new RxUDPSever(8888, map);
	}

}
