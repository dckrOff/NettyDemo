package ifreecomm.nettydemo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.littlegreens.netty.client.NettyTcpClient;
import com.littlegreens.netty.client.listener.NettyClientListener;
import com.littlegreens.netty.client.status.ConnectState;

import ifreecomm.nettydemo.adapter.LogAdapter;
import ifreecomm.nettydemo.bean.LogBean;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        EditText etIpAddress = findViewById(R.id.et_ip_address);
        EditText etPort = findViewById(R.id.et_port);
        Button btnSave = findViewById(R.id.btn_save);

        btnSave.setOnClickListener(v -> {
            if (!etIpAddress.getText().toString().trim().isEmpty() || !etPort.getText().toString().trim().isEmpty()) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("ip_address", etIpAddress.getText().toString());
                intent.putExtra("port", Integer.parseInt(etPort.getText().toString()));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Заполните все поля ввода!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}