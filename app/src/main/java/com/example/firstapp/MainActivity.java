package com.example.firstapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

public class MainActivity extends AppCompatActivity {
    private BigInteger a, b, gab;
    private Random rand;
    PublicInfo _public;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // necemo da dozvolimo screenshot-ove
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // kontrole
        final Button btnDecipher = findViewById(R.id.btnDecipher);
        final Button btnGen = findViewById(R.id.genKeys);
        final TextView txt = findViewById(R.id.editText);
        final Button btnSend = findViewById(R.id.btnSend);
        final TextView labelGAB = findViewById(R.id.labelGAB);
        final Button btnUcitaj = findViewById(R.id.btnUcitaj);

        gab = null;

        // ne dozvoljamo sifrovanje/desifrovanje pre razmene kljuceva
        btnDecipher.setEnabled(false);
        btnSend.setEnabled(false);

        // handler za slanje sifrovanje poruke
        btnSend.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    String text = txt.getText().toString();
                    txt.setText("");
                    share("Пошаљи шифровано користећи", AES.encrypt(text, gab.toString()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        // handler za generisanje p, g, a, g^a
        btnGen.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Pair<BigInteger, BigInteger> pair = findPrimeAndPrimeRoot();
                rand = new Random();

                a = BigInteger.valueOf(rand.nextInt() & Integer.MAX_VALUE);
                _public = new PublicInfo(pair.p, pair.g, pair.g.modPow(a, pair.p));

                String text = _public.p.toString(16)
                        + "\n" + _public.g.toString(16)
                        + "\n" + _public.ga.toString(16);

                share("Пошаљи p, g, g^a користећи", text);
            }
        });

        // handler za ucitavanje kljuca (ili p, g, g^a, ili g^b, zavisno od toga do kojeg smo koraka)
        btnUcitaj.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String data = getClipboard("Нисте копирали кључ.");

                if (data == null)
                    return;

                String[] d = data.split("\n");

                // primili smo p, g, g^a
                if (d.length == 3)
                {
                    _public = new PublicInfo(new BigInteger(d[0], 16), new BigInteger(d[1], 16), new BigInteger(d[2], 16));

                    rand = new Random();
                    b = BigInteger.valueOf(rand.nextInt() & Integer.MAX_VALUE);

                    _public.gb = _public.g.modPow(b, _public.p);
                    gab = _public.ga.modPow(b, _public.p);

                    labelGAB.setText(gab.toString());

                    btnDecipher.setEnabled(true);
                    btnSend.setEnabled(true);

                    share("Пошаљи g^b користећи", _public.gb.toString(16));
                    return;
                }
                // primili smo g^b
                else if (d.length == 1)
                {
                    gab = (new BigInteger(d[0], 16)).modPow(a, _public.p);

                    labelGAB.setText(gab.toString());

                    btnDecipher.setEnabled(true);
                    btnSend.setEnabled(true);

                    toastr("Кључеви учитани!");
                }
                else
                {
                    toastr("Ти не знаш шта радиш!");
                }
            }
        });

        // handler za desifrovanje
        btnDecipher.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String data = getClipboard("Нисте копирали поруку.");

                if (data != null) {
                    try {
                        String dec = AES.decrypt(data, gab.toString());
                        toastr(dec);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////
    //////////////// Pomocne funkcije
    ////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private void share(String title, String text)
    {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, title));
    }

    private String getClipboard(String wrongFormat)
    {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        // If it does contain data, decide if you can handle the data.
        if (!(clipboard.hasPrimaryClip())) {
            toastr("Нисте ништа копирали.");
        } else if (!(clipboard.getPrimaryClipDescription().hasMimeType(MIMETYPE_TEXT_PLAIN))) {
            toastr(wrongFormat);
        } else {
            ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
            return item.getText().toString();
        }

        return null;
    }

    static Pair<BigInteger, BigInteger> findPrimeAndPrimeRoot()
    {
        BigInteger two = BigInteger.valueOf(2);
        BigInteger one = BigInteger.ONE;
        BigInteger p;
        BigInteger q;
        int bitLength = 256;
        int certainty = 20;
        Random rand = new Random();
        while (true){
            q = new BigInteger(bitLength, certainty, rand);
            p = q.multiply(two).add(one);

            if (p.isProbablePrime(certainty)) {
                for (int i = 0; i < 1000; i++) {
                    BigInteger g = BigInteger.valueOf(rand.nextInt(100000) + 1);

                    if (!(g.modPow(two, p).equals(one))&&!(g.modPow(q, p).equals(one))){
                        return new Pair<>(p, g);
                    }
                }
            }
        }
    }

    private void toastr(String s)
    {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }
}
