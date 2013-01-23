#-*- coding:utf-8 -*-

import socket
import select
import threading
import sys
from common import *

class ChatClient:
	_name = None
	_server = None
	_sock = None
	_listenServerThread = None
	_inputMsgThread = None
	_end = False

	def start(self, name, server):
		self._name = name
		self._server = (server, SERVER_PORT)

		self._sock = socket.socket()
		self._sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

		if self._connectServer():
			print "Connected to %s !" % (str(self._server))
			# start threads
			self._listenServerThread = threading.Thread(target = self._listenServer)
			self._inputMsgThread = threading.Thread(target = self._inputMsg)

			self._listenServerThread.start()
			self._inputMsgThread.start()

			return True
		else:
			self._sock.close()
			return False

	def stop(self):
		self._end = True

	def _listenServer(self):
		lastID = MSG_MAX_KEEP - 1;

		while not self._end:
			data = self._receiveMsg(timeout = MSG_TIMEOUT)
			if data == None:
				continue

			# extract clients and messages
			try:
				clientData, msgData = data.strip().split("#")
			except:
				continue
			clients = clientData.split(MSG_SEP)
			messages = []
			for i in msgData.split(MSG_SEP):
				try:
					msgID, msgClient, msgTxt = i.split("@")
					messages.append((int(msgID), msgClient, msgTxt))
				except:
					pass

			if len(messages) > 0 and (messages[-1][0] != lastID):
				lastID = messages[-1][0]
				print "New message !"
				print "Clients: %s" % (str(clients))
				for i in messages:
					print "%s: %s" % (i[1], i[2])
				print

	
	def _inputMsg(self):
		while not self._end:
			msg = sys.stdin.readline()
			self._sendMsg(msg.strip())			

	def _connectServer(self):
		self._sock.settimeout(CONNECT_TIMEOUT)
		try:
			self._sock.connect(self._server)
			if not self._sendMsg(self._name):
				return False
			return True
		except socket.timeout:
			return False

	# receive msg
	def _receiveMsg(self, timeout = None, sock = None):
		if sock == None:
			sock = self._sock
		sock.settimeout(timeout)
		try:
			return sock.recv(MSG_MAX_BYTES)
		except:
			return None

	# send msg
	def _sendMsg(self, msg):
		try:
			self._sock.sendall(msg)
			return True
		except:
			return False



