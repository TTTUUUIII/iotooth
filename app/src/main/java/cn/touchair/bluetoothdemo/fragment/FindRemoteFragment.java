/*
 * Copyright ©️2023 www.touchair.cn
 * Create by <de.liu@touchair.cn>
 */

package cn.touchair.bluetoothdemo.fragment;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cn.touchair.bluetoothdemo.CentralActivity;
import cn.touchair.bluetoothdemo.R;
import cn.touchair.bluetoothdemo.databinding.FragmentFindRemoteBinding;
import cn.touchair.bluetoothdemo.databinding.ItemRemoteViewBinding;
import cn.touchair.iotooth.central.ScanResultCallback;

public class FindRemoteFragment extends Fragment implements ScanResultCallback, View.OnClickListener {
    private FragmentFindRemoteBinding binding;
    private CentralActivity mParent;
    private List<ScanResult> mRemoteList = new ArrayList<>();
    private RemoteViewAdapter mAdapter = new RemoteViewAdapter();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        binding = FragmentFindRemoteBinding.inflate(inflater, container, false);
        binding.scanBtn.setOnClickListener(this::onClick);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireActivity()));
        binding.recyclerView.setAdapter(mAdapter);
        ActionBar actionBar = mParent.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setSubtitle("");
        }
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        mRemoteList.clear();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mParent = (CentralActivity) context;
    }

    @Override
    public void onScanStarted() {
        binding.textView.setText("状态：扫描中");
    }

    @Override
    public void onScanStopped() {
        binding.textView.setText("状态：已停止");
    }

    @Override
    public void onScanResult(ScanResult result) {
        BluetoothDevice device = result.getDevice();
        String address = device.getAddress();
        if (!contains(address)) {
            mRemoteList.add(result);
            mAdapter.notifyItemInserted(mRemoteList.size());
        }
    }

    private boolean contains(@NonNull String address) {
        for (ScanResult result : mRemoteList) {
            if (result.getDevice().getAddress().equals(address)) {
                return true;
            }
        }
        return false;
    }

    public static FindRemoteFragment newInstance() {
        FindRemoteFragment fragment = new FindRemoteFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.scan_btn) {
            if (Objects.nonNull(mParent)) {
                mRemoteList.clear();
                mAdapter.notifyDataSetChanged();
                mParent.startScan(this);
            }
        }
    }

    private class RemoteViewHolder extends RecyclerView.ViewHolder {
        private ItemRemoteViewBinding itemBinding;
        public RemoteViewHolder(@NonNull ItemRemoteViewBinding itemBinding) {
            super(itemBinding.getRoot());
            this.itemBinding = itemBinding;
        }

        public void bind(@NonNull ScanResult result) {
            BluetoothDevice device = result.getDevice();
            @SuppressLint("MissingPermission") String name = device.getName();
            if (Objects.isNull(name)) name = "Unknown";
            String address = device.getAddress();
            itemBinding.deviceName.setText(name);
            itemBinding.deviceAddress.setText(address);
            itemBinding.rssiTextView.setText(String.valueOf(result.getRssi()));
            itemBinding.connectBtn.setOnClickListener((view) -> {
                CommunicateFragment communicateFragment = CommunicateFragment.newInstance();
                Bundle arguments = communicateFragment.getArguments();
                assert arguments != null;
                arguments.putParcelable(CommunicateFragment.ARG_KEY_CONNECT_TO, device);
                mParent.startFragment(communicateFragment);
            });
        }
    }

    private class RemoteViewAdapter extends RecyclerView.Adapter<RemoteViewHolder> {

        @NonNull
        @Override
        public RemoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemRemoteViewBinding itemBinding = ItemRemoteViewBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent,
                    false
            );
            return new RemoteViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull RemoteViewHolder holder, int position) {
            holder.bind(mRemoteList.get(position));
        }

        @Override
        public int getItemCount() {
            return mRemoteList.size();
        }
    }
}
